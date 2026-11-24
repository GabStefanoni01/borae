package com.borae.assistant;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Estrutura normalizada de saída devolvida por qualquer adaptador de IA ao orquestrador.
 *
 * <p>Preserva todas as tentativas efetivamente executadas ({@link AttemptUsage}), distinguindo
 * explicitamente entre uso conhecido ({@code usageKnown=true}, inclusive quando o consumo real
 * for 0 tokens) e metadados ausentes ({@code usageKnown=false}).</p>
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
                    : List.of(AttemptUsage.known(provider, inputTokens, outputTokens));
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
        return of(provider, answer, recommendations, true, inputTokens, outputTokens, 0L);
    }

    public static AiProviderResponse of(
            AiProviderId provider,
            String answer,
            List<RecommendationCandidate> recommendations,
            boolean usageKnown,
            long inputTokens,
            long outputTokens,
            long reasoningTokens
    ) {
        long safeIn = usageKnown ? Math.max(0L, inputTokens) : 0L;
        long safeOut = usageKnown ? Math.max(0L, outputTokens) : 0L;
        AttemptUsage attempt = provider == null
                ? null
                : (usageKnown
                        ? AttemptUsage.known(provider, safeIn, safeOut, reasoningTokens)
                        : AttemptUsage.unknown(provider));
        return new AiProviderResponse(
                provider,
                answer,
                recommendations,
                safeIn,
                safeOut,
                false,
                null,
                attempt == null ? List.of() : List.of(attempt)
        );
    }

    public AiProviderResponse asFallbackFor(AiProviderFailure primaryFailure) {
        List<AttemptUsage> combined = new ArrayList<>();
        if (primaryFailure != null && primaryFailure.attemptExecuted()) {
            combined.add(primaryFailure.toAttemptUsage());
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

    public record AttemptUsage(
            AiProviderId provider,
            boolean usageKnown,
            long inputTokens,
            long outputTokens,
            long reasoningTokens
    ) {
        public AttemptUsage {
            inputTokens = usageKnown ? Math.max(0L, inputTokens) : 0L;
            outputTokens = usageKnown ? Math.max(0L, outputTokens) : 0L;
            reasoningTokens = usageKnown ? Math.max(0L, reasoningTokens) : 0L;
        }

        public AttemptUsage(AiProviderId provider, long inputTokens, long outputTokens) {
            this(provider, true, inputTokens, outputTokens, 0L);
        }

        public static AttemptUsage known(AiProviderId provider, long inputTokens, long outputTokens) {
            return new AttemptUsage(provider, true, inputTokens, outputTokens, 0L);
        }

        public static AttemptUsage known(AiProviderId provider, long inputTokens, long outputTokens, long reasoningTokens) {
            return new AttemptUsage(provider, true, inputTokens, outputTokens, reasoningTokens);
        }

        public static AttemptUsage unknown(AiProviderId provider) {
            return new AttemptUsage(provider, false, 0L, 0L, 0L);
        }

        public boolean hasTokens() {
            return usageKnown && (inputTokens > 0L || outputTokens > 0L || reasoningTokens > 0L);
        }
    }
}
