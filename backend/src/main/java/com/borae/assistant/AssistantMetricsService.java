package com.borae.assistant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Métricas agregadas e privacidade (D-11) do Assistente Gastronômico.
 *
 * <p>Contabiliza tokens e estimativa de custo estritamente por provedor usando a tabela
 * existente {@code assistant_daily_metrics}. Diferencia explicitamente uso desconhecido
 * ({@code usageKnown=false}) de consumo real de zero tokens ({@code usageKnown=true, 0/0}):
 * tentativas realizadas sem metadados registram {@code PROVIDER_*_USAGE_UNKNOWN} e
 * {@code ESTIMATED_COST_UNAVAILABLE} sem inventar tokens nem gravar custo zero fictício.</p>
 */
@Service
public class AssistantMetricsService {

    public static final int RETENTION_DAYS = 180;
    public static final double UNCONFIGURED_PRICE = -1.0;

    private final JdbcTemplate jdbcTemplate;
    private final double geminiInputPricePerMillionUsd;
    private final double geminiOutputPricePerMillionUsd;
    private final double grokInputPricePerMillionUsd;
    private final double grokOutputPricePerMillionUsd;

    @Autowired
    public AssistantMetricsService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.assistant.gemini.input-price-per-million-usd:-1}") double geminiInputPricePerMillionUsd,
            @Value("${app.assistant.gemini.output-price-per-million-usd:-1}") double geminiOutputPricePerMillionUsd,
            @Value("${app.assistant.grok.input-price-per-million-usd:-1}") double grokInputPricePerMillionUsd,
            @Value("${app.assistant.grok.output-price-per-million-usd:-1}") double grokOutputPricePerMillionUsd
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.geminiInputPricePerMillionUsd = geminiInputPricePerMillionUsd;
        this.geminiOutputPricePerMillionUsd = geminiOutputPricePerMillionUsd;
        this.grokInputPricePerMillionUsd = grokInputPricePerMillionUsd;
        this.grokOutputPricePerMillionUsd = grokOutputPricePerMillionUsd;
    }

    /**
     * Sobrecarga compatível para registro simples (assume Gemini como origem dos tokens).
     */
    @Transactional
    public void recordInteractionOutcome(
            UUID restaurantId,
            long latencyMillis,
            boolean fallback,
            boolean noResult,
            List<UUID> recommendedProductIds,
            long inputTokens,
            long outputTokens
    ) {
        List<AiProviderResponse.AttemptUsage> attempts = (inputTokens > 0 || outputTokens > 0)
                ? List.of(AiProviderResponse.AttemptUsage.known(AiProviderId.GEMINI, inputTokens, outputTokens))
                : List.of();
        recordInteractionOutcome(restaurantId, latencyMillis, fallback, noResult, recommendedProductIds, attempts);
    }

    /**
     * Registra desfecho da interação com detalhamento fiel de tokens e custo estimado
     * por tentativa efetivamente realizada (inclusive tentativas falhas sem metadados de uso).
     */
    @Transactional
    public void recordInteractionOutcome(
            UUID restaurantId,
            long latencyMillis,
            boolean fallback,
            boolean noResult,
            List<UUID> recommendedProductIds,
            List<AiProviderResponse.AttemptUsage> attempts
    ) {
        LocalDate today = LocalDate.now(AssistantQuotaService.BUSINESS_ZONE);
        purgeExpired(today);

        incrementMetric(today, restaurantId, "INTERACTIONS", 1);
        incrementMetric(today, restaurantId, "LATENCY_TOTAL_MS", Math.max(0, latencyMillis));
        incrementMetric(today, restaurantId, "LATENCY_SAMPLES", 1);

        if (fallback) incrementMetric(today, restaurantId, "FALLBACKS", 1);
        if (noResult) incrementMetric(today, restaurantId, "NO_RESULTS", 1);

        if (attempts != null) {
            for (AiProviderResponse.AttemptUsage attempt : attempts) {
                recordAttemptUsage(today, restaurantId, attempt);
            }
        }

        if (recommendedProductIds != null) {
            for (UUID productId : recommendedProductIds) {
                if (productId != null) incrementProductMetric(today, restaurantId, productId, "recommendation_count");
            }
        }
    }

    private void recordAttemptUsage(LocalDate today, UUID restaurantId, AiProviderResponse.AttemptUsage attempt) {
        if (attempt == null || attempt.provider() == null) {
            return;
        }
        String prefix = providerMetricPrefix(attempt.provider());

        if (!attempt.usageKnown()) {
            incrementMetric(today, restaurantId, prefix + "_USAGE_UNKNOWN", 1);
            incrementMetric(today, restaurantId, prefix + "_COST_UNAVAILABLE", 1);
            incrementMetric(today, restaurantId, "ESTIMATED_COST_UNAVAILABLE", 1);
            return;
        }

        long inputTokens = attempt.inputTokens();
        long outputTokens = attempt.outputTokens();
        long reasoningTokens = attempt.reasoningTokens();

        if (inputTokens > 0) {
            incrementMetric(today, restaurantId, "INPUT_TOKENS", inputTokens);
            incrementMetric(today, restaurantId, prefix + "_INPUT_TOKENS", inputTokens);
        }
        if (outputTokens > 0) {
            incrementMetric(today, restaurantId, "OUTPUT_TOKENS", outputTokens);
            incrementMetric(today, restaurantId, prefix + "_OUTPUT_TOKENS", outputTokens);
        }
        if (reasoningTokens > 0) {
            incrementMetric(today, restaurantId, prefix + "_REASONING_TOKENS", reasoningTokens);
        }

        double inputPrice = inputPriceFor(attempt.provider());
        double outputPrice = outputPriceFor(attempt.provider());

        if (inputPrice < 0.0 || outputPrice < 0.0) {
            incrementMetric(today, restaurantId, "ESTIMATED_COST_UNAVAILABLE", 1);
            incrementMetric(today, restaurantId, prefix + "_COST_UNAVAILABLE", 1);
            return;
        }

        long estimatedCostMicroUsd = Math.round(inputTokens * inputPrice + outputTokens * outputPrice);
        if (estimatedCostMicroUsd > 0) {
            incrementMetric(today, restaurantId, "ESTIMATED_COST_MICRO_USD", estimatedCostMicroUsd);
            incrementMetric(today, restaurantId, prefix + "_COST_MICRO_USD", estimatedCostMicroUsd);
        }
    }

    private double inputPriceFor(AiProviderId providerId) {
        return switch (providerId) {
            case GEMINI -> geminiInputPricePerMillionUsd;
            case GROK -> grokInputPricePerMillionUsd;
        };
    }

    private double outputPriceFor(AiProviderId providerId) {
        return switch (providerId) {
            case GEMINI -> geminiOutputPricePerMillionUsd;
            case GROK -> grokOutputPricePerMillionUsd;
        };
    }

    private static String providerMetricPrefix(AiProviderId providerId) {
        return switch (providerId) {
            case GEMINI -> "PROVIDER_GEMINI";
            case GROK -> "PROVIDER_GROK";
        };
    }

    @Transactional
    public void recordProviderError(UUID restaurantId) {
        LocalDate today = LocalDate.now(AssistantQuotaService.BUSINESS_ZONE);
        purgeExpired(today);
        incrementMetric(today, restaurantId, "PROVIDER_ERRORS", 1);
    }

    @Transactional
    public void recordProviderSuccess(UUID restaurantId, AiProviderId providerId, boolean usedFallback) {
        LocalDate today = LocalDate.now(AssistantQuotaService.BUSINESS_ZONE);
        purgeExpired(today);
        if (providerId == AiProviderId.GEMINI) {
            incrementMetric(today, restaurantId, "PROVIDER_GEMINI_SUCCESS", 1);
        } else if (providerId == AiProviderId.GROK) {
            incrementMetric(today, restaurantId, "PROVIDER_GROK_SUCCESS", 1);
        }
        if (usedFallback) {
            incrementMetric(today, restaurantId, "PROVIDER_FALLBACK_TO_GROK", 1);
        }
    }

    @Transactional
    public void recordSafetyRefusal(UUID restaurantId) {
        LocalDate today = LocalDate.now(AssistantQuotaService.BUSINESS_ZONE);
        purgeExpired(today);
        incrementMetric(today, restaurantId, "PROVIDER_SAFETY_REFUSALS", 1);
    }

    @Transactional
    public void recordProductClick(UUID restaurantId, UUID productId) {
        LocalDate today = LocalDate.now(AssistantQuotaService.BUSINESS_ZONE);
        purgeExpired(today);
        incrementProductMetric(today, restaurantId, productId, "click_count");
    }

    private void incrementMetric(LocalDate date, UUID restaurantId, String key, long amount) {
        jdbcTemplate.update("""
                INSERT INTO assistant_daily_metrics (metric_date, restaurant_id, metric_key, metric_value, updated_at)
                VALUES (?, ?, ?, ?, NOW())
                ON CONFLICT (metric_date, restaurant_id, metric_key)
                DO UPDATE SET metric_value = assistant_daily_metrics.metric_value + EXCLUDED.metric_value,
                              updated_at = NOW()
                """, date, restaurantId, key, amount);
    }

    private void incrementProductMetric(LocalDate date, UUID restaurantId, UUID productId, String column) {
        if (!"recommendation_count".equals(column) && !"click_count".equals(column)) {
            throw new IllegalArgumentException("Métrica de produto inválida.");
        }
        jdbcTemplate.update("""
                INSERT INTO assistant_daily_product_metrics
                    (metric_date, restaurant_id, product_id, recommendation_count, click_count, updated_at)
                VALUES (?, ?, ?, ?, ?, NOW())
                ON CONFLICT (metric_date, restaurant_id, product_id)
                DO UPDATE SET recommendation_count =
                                  assistant_daily_product_metrics.recommendation_count + EXCLUDED.recommendation_count,
                              click_count =
                                  assistant_daily_product_metrics.click_count + EXCLUDED.click_count,
                              updated_at = NOW()
                """, date, restaurantId, productId,
                "recommendation_count".equals(column) ? 1 : 0,
                "click_count".equals(column) ? 1 : 0);
    }

    @Scheduled(cron = "0 15 3 * * *", zone = "America/Sao_Paulo")
    @Transactional
    public void purgeExpiredMetrics() {
        purgeExpired(LocalDate.now(AssistantQuotaService.BUSINESS_ZONE));
    }

    private void purgeExpired(LocalDate today) {
        LocalDate cutoff = today.minusDays(RETENTION_DAYS);
        jdbcTemplate.update("DELETE FROM assistant_daily_metrics WHERE metric_date < ?", cutoff);
        jdbcTemplate.update("DELETE FROM assistant_daily_product_metrics WHERE metric_date < ?", cutoff);
    }
}
