package com.borae.assistant;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Estrutura normalizada de saída devolvida por qualquer adaptador de IA ao orquestrador.
 * Inclui o detalhamento de consumo por tentativa ({@link AttemptUsage}) para cálculo fiel
 * de tokens e custos estimados por provedor mesmo quando ocorre fallback.
 */
public record AiProviderResponse(
        AiProviderId provider,
        String answer,
        List<RecommendationCandidate> recommendations,
        long inputTokens,
        long outputTokens,
        boolean usedFallbackProvider,
        String primaryFailureCode,
        List<AttemptUsage> attempts
) {
    public AiProviderResponse {
        answer = answer == null ? "" : answer;
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
        inputTokens = Math.max(0L, inputTokens);
        outputTokens = Math.max(0L, outputTokens);
        if (attempts == null || attempts.isEmpty()) {
            attempts = provider == null
                    ? List.of()
                    : List.of(new AttemptUsage(provider, inputTokens, outputTokens));
        } else {
            attempts = List.copyOf(attempts);
        }
    }

    public static AiProviderResponse of(
            AiProviderId provider,
            String answer,
            List<RecommendationCandidate> recommendations,
            long inputTokens,
            long outputTokens
    ) {
        long safeIn = Math.max(0L, inputTokens);
        long safeOut = Math.max(0L, outputTokens);
        return new AiProviderResponse(
                provider,
                answer,
                recommendations,
                safeIn,
                safeOut,
                false,
                null,
                provider == null ? List.of() : List.of(new AttemptUsage(provider, safeIn, safeOut))
        );
    }

    public AiProviderResponse asFallbackFor(AiProviderFailure primaryFailure) {
        List<AttemptUsage> combined = new ArrayList<>();
        if (primaryFailure != null && primaryFailure.hasTokenUsage()) {
            combined.add(new AttemptUsage(
                    primaryFailure.provider(),
                    primaryFailure.inputTokens(),
                    primaryFailure.outputTokens()
            ));
        }
        combined.addAll(attempts);
        return new AiProviderResponse(
                provider,
                answer,
                recommendations,
                inputTokens,
                outputTokens,
                true,
                primaryFailure == null ? null : primaryFailure.type().name(),
                combined
        );
    }

    public record RecommendationCandidate(UUID productId, String reason) {}

    public record AttemptUsage(AiProviderId provider, long inputTokens, long outputTokens) {
        public AttemptUsage {
            inputTokens = Math.max(0L, inputTokens);
            outputTokens = Math.max(0L, outputTokens);
        }

        public boolean hasTokens() {
            return inputTokens > 0L || outputTokens > 0L;
        }
    }
}
