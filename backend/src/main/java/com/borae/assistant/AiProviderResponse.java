package com.borae.assistant;

import java.util.List;
import java.util.UUID;

/**
 * Estrutura normalizada de saída devolvida por qualquer adaptador de IA ao orquestrador
 * e posteriormente validada de forma centralizada pelo backend.
 */
public record AiProviderResponse(
        AiProviderId provider,
        String answer,
        List<RecommendationCandidate> recommendations,
        long inputTokens,
        long outputTokens,
        boolean usedFallbackProvider,
        String primaryFailureCode
) {
    public AiProviderResponse {
        answer = answer == null ? "" : answer;
        recommendations = recommendations == null ? List.of() : List.copyOf(recommendations);
        inputTokens = Math.max(0L, inputTokens);
        outputTokens = Math.max(0L, outputTokens);
    }

    public static AiProviderResponse of(
            AiProviderId provider,
            String answer,
            List<RecommendationCandidate> recommendations,
            long inputTokens,
            long outputTokens
    ) {
        return new AiProviderResponse(provider, answer, recommendations, inputTokens, outputTokens, false, null);
    }

    public AiProviderResponse asFallbackFor(AiProviderFailure primaryFailure) {
        return new AiProviderResponse(
                provider,
                answer,
                recommendations,
                inputTokens,
                outputTokens,
                true,
                primaryFailure == null ? null : primaryFailure.type().name()
        );
    }

    public record RecommendationCandidate(UUID productId, String reason) {}
}
