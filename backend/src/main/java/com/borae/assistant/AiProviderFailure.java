package com.borae.assistant;

/**
 * Classificação normalizada de falhas de provedores de IA.
 * Determina objetivamente quais falhas técnicas podem acionar o provedor secundário (Grok)
 * e quais jamais devem acionar fallback (ex.: recusa de segurança ou erro de entrada).
 */
public record AiProviderFailure(
        AiProviderId provider,
        FailureType type,
        Integer httpStatus,
        String safeDetail
) {

    public enum FailureType {
        TIMEOUT(true, true),
        HTTP_5XX(true, true),
        RATE_LIMITED(true, true),
        SERVICE_UNAVAILABLE(true, true),
        MALFORMED_RESPONSE(true, true),
        AUTH_OR_CONFIG_ERROR(true, true),
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

        public AiProviderException(AiProviderId provider, FailureType type, Integer httpStatus, String safeDetail) {
            super(safeDetail != null && !safeDetail.isBlank()
                    ? safeDetail
                    : "Falha no provedor de IA (" + provider.key() + "): " + type.name());
            this.failure = new AiProviderFailure(provider, type, httpStatus, getMessage());
        }

        public AiProviderException(AiProviderId provider, FailureType type, String safeDetail) {
            this(provider, type, null, safeDetail);
        }

        public AiProviderFailure getFailure() {
            return failure;
        }
    }
}
