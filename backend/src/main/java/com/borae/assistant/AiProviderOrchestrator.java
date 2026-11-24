package com.borae.assistant;

import com.borae.assistant.AiProviderFailure.AiProviderException;
import com.borae.assistant.AiProviderFailure.FailureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.LongSupplier;

/**
 * Orquestrador multi-provider do Assistente Gastronômico.
 *
 * <p>Aplica rigorosamente a prioridade determinística:
 * <strong>Prioridade 1 — Gemini → Prioridade 2 — Grok (xAI)</strong>, compartilhando
 * um {@link AiRequestDeadline} absoluto por solicitação para que o timeout HTTP efetivo
 * de cada provedor jamais ultrapasse o orçamento global ({@code AI_TOTAL_TIMEOUT_MS}).</p>
 *
 * <p>Recusas de segurança ({@link FailureType#SAFETY_REFUSAL}), solicitações inválidas
 * ({@link FailureType#INVALID_USER_REQUEST}) e (por padrão) erros de autenticação/configuração
 * ({@link FailureType#AUTH_OR_CONFIG_ERROR}) não acionam fallback para o Grok.</p>
 */
@Service
public class AiProviderOrchestrator {

    private static final Logger LOG = LoggerFactory.getLogger(AiProviderOrchestrator.class);

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
     * Executa a recomendação respeitando estritamente a ordem Gemini → Grok e o deadline global.
     */
    public AiProviderResponse recommend(AiProviderRequest request) {
        AiRequestDeadline deadline = request.deadline() != null
                ? request.deadline()
                : AiRequestDeadline.start(configuration.totalTimeoutMs(), configuration.minFallbackBudgetMs(), clock);

        if (deadline.isExpired()) {
            throw new AiProviderException(AiProviderId.GEMINI, FailureType.TIMEOUT,
                    "Orçamento global de tempo expirado antes da chamada de IA.");
        }

        AiProviderRequest scopedRequest = request.withDeadline(deadline);
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
                AiProviderResponse response = primaryProvider.recommend(scopedRequest);
                primaryBreaker.recordSuccess();
                return response;
            } catch (AiProviderException exception) {
                primaryFailure = exception.getFailure();
                primaryBreaker.recordFailure(primaryFailure.type());
                logFailure(primaryFailure);

                if (!primaryFailure.allowsFallback(configuration.isFallbackOnAuthOrConfigError())) {
                    throw exception;
                }
            }
        }

        // Prioridade 2 — Grok (no máximo 1 tentativa de contingência limitada ao orçamento restante)
        if (!canAttemptFallback(primaryFailure, deadline)) {
            throw toException(primaryFailure);
        }

        LOG.info("AI fallback triggered: primary={} reason={} -> secondary={} remainingBudgetMs={}",
                AiProviderId.GEMINI.key(), primaryFailure.type().name(),
                fallbackProvider.id().key(), deadline.remainingMillis());

        try {
            AiProviderResponse secondaryResponse = fallbackProvider.recommend(scopedRequest);
            fallbackBreaker.recordSuccess();
            return secondaryResponse.asFallbackFor(primaryFailure);
        } catch (AiProviderException secondaryException) {
            AiProviderFailure secondaryFailure = secondaryException.getFailure();
            fallbackBreaker.recordFailure(secondaryFailure.type());
            logFailure(secondaryFailure);
            throw secondaryException.withPriorAttemptUsage(primaryFailure);
        }
    }

    private boolean canAttemptFallback(AiProviderFailure primaryFailure, AiRequestDeadline deadline) {
        if (!configuration.isFallbackEnabled() || fallbackProvider == null) {
            return false;
        }
        if (primaryFailure == null || !primaryFailure.allowsFallback(configuration.isFallbackOnAuthOrConfigError())) {
            return false;
        }
        if (deadline.isExpired() || !deadline.hasOperationalBudget()) {
            LOG.warn("AI fallback skipped: total deadline budget exhausted (elapsedMs={}, remainingMs={}, budgetMs={})",
                    deadline.elapsedMillis(), deadline.remainingMillis(), deadline.totalBudgetMs());
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
                failure.safeDetail(),
                failure.inputTokens(),
                failure.outputTokens(),
                List.of()
        );
    }
}
