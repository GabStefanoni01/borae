package com.borae.assistant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AssistantMetricsService {

    public static final int RETENTION_DAYS = 180;

    private final JdbcTemplate jdbcTemplate;
    private final double inputPricePerMillionUsd;
    private final double outputPricePerMillionUsd;

    public AssistantMetricsService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.assistant.gemini.input-price-per-million-usd:0.10}") double inputPricePerMillionUsd,
            @Value("${app.assistant.gemini.output-price-per-million-usd:0.40}") double outputPricePerMillionUsd
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.inputPricePerMillionUsd = Math.max(0, inputPricePerMillionUsd);
        this.outputPricePerMillionUsd = Math.max(0, outputPricePerMillionUsd);
    }

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
        LocalDate today = LocalDate.now(AssistantQuotaService.BUSINESS_ZONE);
        purgeExpired(today);

        incrementMetric(today, restaurantId, "INTERACTIONS", 1);
        incrementMetric(today, restaurantId, "LATENCY_TOTAL_MS", Math.max(0, latencyMillis));
        incrementMetric(today, restaurantId, "LATENCY_SAMPLES", 1);

        if (fallback) incrementMetric(today, restaurantId, "FALLBACKS", 1);
        if (noResult) incrementMetric(today, restaurantId, "NO_RESULTS", 1);
        if (inputTokens > 0) incrementMetric(today, restaurantId, "INPUT_TOKENS", inputTokens);
        if (outputTokens > 0) incrementMetric(today, restaurantId, "OUTPUT_TOKENS", outputTokens);
        long estimatedCostMicroUsd = Math.round(
                inputTokens * inputPricePerMillionUsd + outputTokens * outputPricePerMillionUsd
        );
        if (estimatedCostMicroUsd > 0) {
            incrementMetric(today, restaurantId, "ESTIMATED_COST_MICRO_USD", estimatedCostMicroUsd);
        }

        if (recommendedProductIds != null) {
            for (UUID productId : recommendedProductIds) {
                if (productId != null) incrementProductMetric(today, restaurantId, productId, "recommendation_count");
            }
        }
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
