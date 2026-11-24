package com.borae.assistant;

import java.util.ArrayList;
import java.util.List;

/**
 * Classificação normalizada de falhas de provedores de IA.
 *
 * <p>Diferencia explicitamente:
 * <ul>
 *   <li>Tentativa efetivamente realizada com uso conhecido ({@code attemptExecuted=true, usageKnown=true})</li>
 *   <li>Tentativa efetivamente realizada sem metadados de uso ({@code attemptExecuted=true, usageKnown=false})</li>
 *   <li>Tentativa não executada (ex.: provider desabilitado, circuit aberto ou deadline expirado antes da chamada:
 *       {@code attemptExecuted=false})</li>
 * </ul></p>
 */
public record AiProviderFailure(
        AiProviderId provider,
        FailureType type,
        Integer httpStatus,
        String safeDetail,
        boolean attemptExecuted,
        boolean usageKnown,
        long inputTokens,
        long outputTokens,
        long reasoningTokens
) {
    public AiProviderFailure {
        usageKnown = attemptExecuted && usageKnown;
        inputTokens = usageKnown ? Math.max(0L, inputTokens) : 0L;
        outputTokens = usageKnown ? Math.max(0L, outputTokens) : 0L;
        reasoningTokens = usageKnown ? Math.max(0L, reasoningTokens) : 0L;
    }

    public AiProviderFailure(AiProviderId provider, FailureType type, Integer httpStatus, String safeDetail) {
        this(provider, type, httpStatus, safeDetail, true, false, 0L, 0L, 0L);
    }

    public static AiProviderFailure skipped(AiProviderId provider, FailureType type, String safeDetail) {
        return new AiProviderFailure(provider, type, null, safeDetail, false, false, 0L, 0L, 0L);
    }

    public boolean hasTokenUsage() {
        return usageKnown && (inputTokens > 0L || outputTokens > 0L || reasoningTokens > 0L);
    }

    public AiProviderResponse.AttemptUsage toAttemptUsage() {
        return usageKnown
                ? AiProviderResponse.AttemptUsage.known(provider, inputTokens, outputTokens, reasoningTokens)
                : AiProviderResponse.AttemptUsage.unknown(provider);
    }

    public enum FailureType {
        TIMEOUT(true, true),
        HTTP_5XX(true, true),
        RATE_LIMITED(true, true),
        SERVICE_UNAVAILABLE(true, true),
        MALFORMED_RESPONSE(true, true),
        AUTH_OR_CONFIG_ERROR(false, true),
        SAFETY_REFUSAL(false, false),
        INVALID_USER_REQUEST(false, false);

        private final boolean fallbackEligibleByDefault;
        private final boolean tripsCircuitBreaker;

        FailureType(boolean fallbackEligibleByDefault, boolean tripsCircuitBreaker) {
            this.fallbackEligibleByDefault = fallbackEligibleByDefault;
            this.tripsCircuitBreaker = tripsCircuitBreaker;
        }

        public boolean isFallbackEligibleByDefault() {
            return fallbackEligibleByDefault;
        }

        public boolean tripsCircuitBreaker() {
            return tripsCircuitBreaker;
        }
    }

    public boolean allowsFallback(boolean fallbackOnAuthOrConfigError) {
        if (type == FailureType.AUTH_OR_CONFIG_ERROR) {
            return fallbackOnAuthOrConfigError;
        }
        return type.isFallbackEligibleByDefault();
    }

    /**
     * Exceção sanitizada de provedor de IA. Nunca inclui chaves, cookies, prompts do usuário
     * ou corpos brutos de erro do fornecedor.
     */
    public static class AiProviderException extends RuntimeException {
        private final AiProviderFailure failure;
        private final List<AiProviderResponse.AttemptUsage> attemptUsages;

        public AiProviderException(AiProviderFailure failure, List<AiProviderResponse.AttemptUsage> priorUsages) {
            super(failure.safeDetail() != null && !failure.safeDetail().isBlank()
                    ? failure.safeDetail()
                    : "Falha no provedor de IA (" + failure.provider().key() + "): " + failure.type().name());
            this.failure = failure;
            List<AiProviderResponse.AttemptUsage> usages = new ArrayList<>();
            if (priorUsages != null) {
                usages.addAll(priorUsages);
            }
            if (failure.attemptExecuted()) {
                usages.add(failure.toAttemptUsage());
            }
            this.attemptUsages = List.copyOf(usages);
        }

        public AiProviderException(AiProviderId provider, FailureType type, Integer httpStatus,
                                   String safeDetail, boolean usageKnown, long inputTokens, long outputTokens,
                                   long reasoningTokens, List<AiProviderResponse.AttemptUsage> priorUsages) {
            this(new AiProviderFailure(provider, type, httpStatus, safeDetail, true,
                    usageKnown, inputTokens, outputTokens, reasoningTokens), priorUsages);
        }

        public AiProviderException(AiProviderId provider, FailureType type, Integer httpStatus,
                                   String safeDetail, long inputTokens, long outputTokens,
                                   List<AiProviderResponse.AttemptUsage> priorUsages) {
            this(provider, type, httpStatus, safeDetail, true, inputTokens, outputTokens, 0L, priorUsages);
        }

        public AiProviderException(AiProviderId provider, FailureType type, Integer httpStatus, String safeDetail) {
            this(new AiProviderFailure(provider, type, httpStatus, safeDetail, true, false, 0L, 0L, 0L), List.of());
        }

        public AiProviderException(AiProviderId provider, FailureType type, String safeDetail) {
            this(provider, type, null, safeDetail);
        }

        public static AiProviderException skipped(AiProviderId provider, FailureType type, String safeDetail) {
            return new AiProviderException(AiProviderFailure.skipped(provider, type, safeDetail), List.of());
        }

        public AiProviderFailure getFailure() {
            return failure;
        }

        public List<AiProviderResponse.AttemptUsage> getAttemptUsages() {
            return attemptUsages;
        }

        public AiProviderException withPriorAttemptUsage(AiProviderFailure priorFailure) {
            if (priorFailure == null || !priorFailure.attemptExecuted()) {
                return this;
            }
            List<AiProviderResponse.AttemptUsage> prior = new ArrayList<>();
            prior.add(priorFailure.toAttemptUsage());
            return new AiProviderException(failure, prior);
        }
    }
}
