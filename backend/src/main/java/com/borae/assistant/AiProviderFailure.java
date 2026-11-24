package com.borae.assistant;

import java.util.List;

/**
 * Classificação normalizada de falhas de provedores de IA.
 * Carrega também eventuais tokens consumidos antes da falha (ex.: resposta malformada ou recusa)
 * para contabilização fiel por provedor sem perder metadados de uso.
 */
public record AiProviderFailure(
        AiProviderId provider,
        FailureType type,
        Integer httpStatus,
        String safeDetail,
        long inputTokens,
        long outputTokens
) {
    public AiProviderFailure {
        inputTokens = Math.max(0L, inputTokens);
        outputTokens = Math.max(0L, outputTokens);
    }

    public AiProviderFailure(AiProviderId provider, FailureType type, Integer httpStatus, String safeDetail) {
        this(provider, type, httpStatus, safeDetail, 0L, 0L);
    }

    public boolean hasTokenUsage() {
        return inputTokens > 0L || outputTokens > 0L;
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

        public AiProviderException(AiProviderId provider, FailureType type, Integer httpStatus,
                                   String safeDetail, long inputTokens, long outputTokens,
                                   List<AiProviderResponse.AttemptUsage> priorUsages) {
            super(safeDetail != null && !safeDetail.isBlank()
                    ? safeDetail
                    : "Falha no provedor de IA (" + provider.key() + "): " + type.name());
            this.failure = new AiProviderFailure(provider, type, httpStatus, getMessage(), inputTokens, outputTokens);
            java.util.ArrayList<AiProviderResponse.AttemptUsage> usages = new java.util.ArrayList<>();
            if (priorUsages != null) {
                usages.addAll(priorUsages);
            }
            if (this.failure.hasTokenUsage()) {
                usages.add(new AiProviderResponse.AttemptUsage(provider, this.failure.inputTokens(), this.failure.outputTokens()));
            }
            this.attemptUsages = List.copyOf(usages);
        }

        public AiProviderException(AiProviderId provider, FailureType type, Integer httpStatus, String safeDetail) {
            this(provider, type, httpStatus, safeDetail, 0L, 0L, List.of());
        }

        public AiProviderException(AiProviderId provider, FailureType type, String safeDetail) {
            this(provider, type, null, safeDetail, 0L, 0L, List.of());
        }

        public AiProviderFailure getFailure() {
            return failure;
        }

        public List<AiProviderResponse.AttemptUsage> getAttemptUsages() {
            return attemptUsages;
        }

        public AiProviderException withPriorAttemptUsage(AiProviderFailure priorFailure) {
            if (priorFailure == null || !priorFailure.hasTokenUsage()) {
                return this;
            }
            java.util.ArrayList<AiProviderResponse.AttemptUsage> combined = new java.util.ArrayList<>();
            combined.add(new AiProviderResponse.AttemptUsage(
                    priorFailure.provider(), priorFailure.inputTokens(), priorFailure.outputTokens()
            ));
            return new AiProviderException(
                    failure.provider(), failure.type(), failure.httpStatus(), failure.safeDetail(),
                    failure.inputTokens(), failure.outputTokens(), combined
            );
        }
    }
}
