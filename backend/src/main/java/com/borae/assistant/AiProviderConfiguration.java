package com.borae.assistant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuração centralizada da política multi-provider (Prioridade 1: Gemini → Prioridade 2: Grok).
 * Preserva a convenção existente em {@code app.assistant.*} sem habilitar provedores por padrão.
 */
@Component
public class AiProviderConfiguration {

    public static final long DEFAULT_TOTAL_TIMEOUT_MS = 15_000L;
    public static final int DEFAULT_CIRCUIT_BREAKER_THRESHOLD = 3;
    public static final long DEFAULT_CIRCUIT_BREAKER_OPEN_MS = 30_000L;
    public static final int DEFAULT_MAX_OUTPUT_TOKENS = 500;

    private final AiProviderId primaryProvider;
    private final AiProviderId fallbackProvider;
    private final boolean fallbackEnabled;
    private final boolean fallbackOnAuthOrConfigError;
    private final long totalTimeoutMs;
    private final int maxOutputTokens;
    private final int circuitBreakerFailureThreshold;
    private final long circuitBreakerOpenDurationMs;

    @Autowired
    public AiProviderConfiguration(
            @Value("${app.assistant.ai.primary-provider:gemini}") String primaryProvider,
            @Value("${app.assistant.ai.fallback-provider:grok}") String fallbackProvider,
            @Value("${app.assistant.ai.fallback-enabled:true}") boolean fallbackEnabled,
            @Value("${app.assistant.ai.fallback-on-auth-or-config-error:true}") boolean fallbackOnAuthOrConfigError,
            @Value("${app.assistant.ai.total-timeout-ms:15000}") long totalTimeoutMs,
            @Value("${app.assistant.gemini.max-output-tokens:500}") int maxOutputTokens,
            @Value("${app.assistant.ai.circuit-breaker.failure-threshold:3}") int circuitBreakerFailureThreshold,
            @Value("${app.assistant.ai.circuit-breaker.open-duration-ms:30000}") long circuitBreakerOpenDurationMs
    ) {
        AiProviderId resolvedPrimary = AiProviderId.fromKey(primaryProvider, AiProviderId.GEMINI);
        AiProviderId resolvedFallback = AiProviderId.fromKey(fallbackProvider, AiProviderId.GROK);
        // Regra arquitetural desta etapa: Gemini é sempre o provedor principal; Grok nunca precede o Gemini.
        if (resolvedPrimary != AiProviderId.GEMINI) {
            throw new IllegalStateException(
                    "Configuração inválida de IA: o provedor principal deve permanecer 'gemini' (Grok atua somente como contingência secundária).");
        }
        this.primaryProvider = resolvedPrimary;
        this.fallbackProvider = resolvedFallback == resolvedPrimary ? AiProviderId.GROK : resolvedFallback;
        this.fallbackEnabled = fallbackEnabled;
        this.fallbackOnAuthOrConfigError = fallbackOnAuthOrConfigError;
        this.totalTimeoutMs = Math.max(1_000L, totalTimeoutMs);
        this.maxOutputTokens = Math.max(64, Math.min(maxOutputTokens, 1_000));
        this.circuitBreakerFailureThreshold = Math.max(1, circuitBreakerFailureThreshold);
        this.circuitBreakerOpenDurationMs = Math.max(1_000L, circuitBreakerOpenDurationMs);
    }

    public static AiProviderConfiguration defaults() {
        return new AiProviderConfiguration(
                "gemini", "grok", true, true,
                DEFAULT_TOTAL_TIMEOUT_MS, DEFAULT_MAX_OUTPUT_TOKENS,
                DEFAULT_CIRCUIT_BREAKER_THRESHOLD, DEFAULT_CIRCUIT_BREAKER_OPEN_MS
        );
    }

    public AiProviderId primaryProvider() { return primaryProvider; }
    public AiProviderId fallbackProvider() { return fallbackProvider; }
    public boolean isFallbackEnabled() { return fallbackEnabled; }
    public boolean isFallbackOnAuthOrConfigError() { return fallbackOnAuthOrConfigError; }
    public long totalTimeoutMs() { return totalTimeoutMs; }
    public int maxOutputTokens() { return maxOutputTokens; }
    public int circuitBreakerFailureThreshold() { return circuitBreakerFailureThreshold; }
    public long circuitBreakerOpenDurationMs() { return circuitBreakerOpenDurationMs; }
}
