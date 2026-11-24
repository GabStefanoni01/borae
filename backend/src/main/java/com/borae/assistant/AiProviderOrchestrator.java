package com.borae.assistant;

import com.borae.assistant.AiProviderFailure.AiProviderException;
import com.borae.assistant.AiProviderFailure.FailureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.function.LongSupplier;

/**
 * Orquestrador multi-provider do Assistente Gastronômico.
 *
 * <p>Aplica rigorosamente a prioridade determinística:
 * <strong>Prioridade 1 — Gemini → Prioridade 2 — Grok (xAI)</strong>.
 * Sem balanceamento aleatório, sem chamadas paralelas entre provedores e com no máximo
 * uma tentativa de contingência ao Grok respeitando o limite total de tempo e circuit breakers
 * independentes por provedor.</p>
 *
 * <p>Recusas de segurança ({@link FailureType#SAFETY_REFUSAL}) e solicitações inválidas
 * ({@link FailureType#INVALID_USER_REQUEST}) jamais acionam fallback para o segundo provedor.</p>
 */
@Service
public class AiProviderOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(AiProviderOrchestrator.class);
    private static final long MIN_REMAINING_BUDGET_FOR_FALLBACK_MS = 500L;

    private final AiProvider primaryProvider;
    private final AiProvider fallbackProvider;
    private final AiProviderConfiguration configuration;
    private final AiProviderCircuitBreaker primaryBreaker;
    private final AiProviderCircuitBreaker fallbackBreaker;
    private final LongSupplier clock;

    @Autowired
    public AiProviderOrchestrator(
            GeminiFlashLiteProvider geminiProvider,
            GrokAiProvider grokProvider,
            AiProviderConfiguration configuration
    ) {
        this(geminiProvider, grokProvider, configuration, System::currentTimeMillis);
    }

    AiProviderOrchestrator(
            AiProvider primaryProvider,
            AiProvider fallbackProvider,
            AiProviderConfiguration configuration,
            LongSupplier clock
    ) {
        if (primaryProvider == null || (primaryProvider.id() != null && primaryProvider.id() != AiProviderId.GEMINI)) {
            throw new IllegalArgumentException("O provedor principal do orquestrador deve ser o Gemini.");
        }
        this.primaryProvider = primaryProvider;
        this.fallbackProvider = fallbackProvider;
        this.configuration = configuration == null ? AiProviderConfiguration.defaults() : configuration;
        this.clock = clock == null ? System::currentTimeMillis : clock;
        this.primaryBreaker = new AiProviderCircuitBreaker(
                AiProviderId.GEMINI,
                this.configuration.circuitBreakerFailureThreshold(),
                this.configuration.circuitBreakerOpenDurationMs(),
                this.clock
        );
        this.fallbackBreaker = new AiProviderCircuitBreaker(
                AiProviderId.GROK,
                this.configuration.circuitBreakerFailureThreshold(),
                this.configuration.circuitBreakerOpenDurationMs(),
                this.clock
        );
    }

    /**
     * Indica se ao menos um provedor elegível está habilitado e configurado para atender requisições.
     */
    public boolean hasAnyAvailableProvider() {
        if (primaryProvider.isAvailable()) {
            return true;
        }
        return configuration.isFallbackEnabled()
                && fallbackProvider != null
                && fallbackProvider.isAvailable();
    }

    public int maxOutputTokens() {
        return configuration.maxOutputTokens();
    }

    public AiProviderCircuitBreaker.State primaryCircuitState() {
        return primaryBreaker.state();
    }

    public AiProviderCircuitBreaker.State fallbackCircuitState() {
        return fallbackBreaker.state();
    }

    public void resetCircuitBreakers() {
        primaryBreaker.reset();
        fallbackBreaker.reset();
    }

    /**
     * Executa a recomendação respeitando estritamente a ordem Gemini → Grok.
     */
    public AiProviderResponse recommend(AiProviderRequest request) {
        long startedAtMs = clock.getAsLong();
        AiProviderFailure primaryFailure = null;

        // Prioridade 1 — Gemini sempre precede o Grok
        if (!primaryProvider.isAvailable()) {
            primaryFailure = new AiProviderFailure(
                    AiProviderId.GEMINI, FailureType.SERVICE_UNAVAILABLE, null,
                    "Provedor principal Gemini indisponível ou desabilitado."
            );
        } else if (!primaryBreaker.allowRequest()) {
            primaryFailure = new AiProviderFailure(
                    AiProviderId.GEMINI, FailureType.SERVICE_UNAVAILABLE, null,
                    "Circuit breaker do provedor principal Gemini aberto."
            );
            LOG.warn("AI provider {} skipped: circuit breaker OPEN", AiProviderId.GEMINI.key());
        } else {
            try {
                AiProviderResponse response = primaryProvider.recommend(request);
                primaryBreaker.recordSuccess();
                return response;
            } catch (AiProviderException exception) {
                primaryFailure = exception.getFailure();
                primaryBreaker.recordFailure(primaryFailure.type());
                logFailure(primaryFailure);

                // Nunca contornar recusa de segurança ou erro de entrada tentando outro modelo
                if (!primaryFailure.allowsFallback(configuration.isFallbackOnAuthOrConfigError())) {
                    throw exception;
                }
            }
        }

        // Prioridade 2 — Grok (no máximo uma tentativa de contingência)
        if (!canAttemptFallback(primaryFailure, startedAtMs)) {
            throw toException(primaryFailure);
        }

        LOG.info("AI fallback triggered: primary={} reason={} -> secondary={}",
                AiProviderId.GEMINI.key(), primaryFailure.type().name(), fallbackProvider.id().key());

        try {
            AiProviderResponse secondaryResponse = fallbackProvider.recommend(request);
            fallbackBreaker.recordSuccess();
            return secondaryResponse.asFallbackFor(primaryFailure);
        } catch (AiProviderException secondaryException) {
            AiProviderFailure secondaryFailure = secondaryException.getFailure();
            fallbackBreaker.recordFailure(secondaryFailure.type());
            logFailure(secondaryFailure);
            throw secondaryException;
        }
    }

    private boolean canAttemptFallback(AiProviderFailure primaryFailure, long startedAtMs) {
        if (!configuration.isFallbackEnabled() || fallbackProvider == null) {
            return false;
        }
        if (primaryFailure == null || !primaryFailure.allowsFallback(configuration.isFallbackOnAuthOrConfigError())) {
            return false;
        }
        long elapsedMs = Math.max(0L, clock.getAsLong() - startedAtMs);
        long remainingBudgetMs = configuration.totalTimeoutMs() - elapsedMs;
        if (remainingBudgetMs < MIN_REMAINING_BUDGET_FOR_FALLBACK_MS) {
            LOG.warn("AI fallback skipped: total time budget exceeded (elapsedMs={}, budgetMs={})",
                    elapsedMs, configuration.totalTimeoutMs());
            return false;
        }
        if (!fallbackProvider.isAvailable()) {
            return false;
        }
        if (!fallbackBreaker.allowRequest()) {
            LOG.warn("AI fallback {} skipped: circuit breaker OPEN", fallbackProvider.id().key());
            return false;
        }
        return true;
    }

    private static void logFailure(AiProviderFailure failure) {
        if (failure.type() == FailureType.AUTH_OR_CONFIG_ERROR) {
            LOG.error("AI operational alert: provider={} type={} httpStatus={}",
                    failure.provider().key(), failure.type().name(), failure.httpStatus());
        } else {
            LOG.warn("AI provider failure: provider={} type={} httpStatus={}",
                    failure.provider().key(), failure.type().name(), failure.httpStatus());
        }
    }

    private static AiProviderException toException(AiProviderFailure failure) {
        return new AiProviderException(
                failure.provider(),
                failure.type(),
                failure.httpStatus(),
                failure.safeDetail()
        );
    }
}
