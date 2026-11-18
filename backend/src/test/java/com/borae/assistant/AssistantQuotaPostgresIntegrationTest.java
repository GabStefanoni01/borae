package com.borae.assistant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@Import({AssistantQuotaService.class, AssistantIpRateLimitService.class, AssistantMetricsService.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AssistantQuotaPostgresIntegrationTest {

    private static final UUID PRODUCT_ID = UUID.fromString("22000000-0000-0000-0000-000000000001");
    private static final UUID RESTAURANT_ID =
            UUID.fromString("21000000-0000-0000-0000-000000000001");

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> true);
        registry.add("app.assistant.rate-limit.secret", () -> "test-rate-limit-secret-with-enough-entropy");
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AssistantQuotaService quotaService;

    @Autowired
    private AssistantIpRateLimitService ipRateLimitService;

    @Autowired
    private AssistantMetricsService metricsService;

    @BeforeEach
    void cleanCounters() {
        jdbc.update("DELETE FROM assistant_daily_quota");
        jdbc.update("DELETE FROM assistant_ip_rate_limit");
        jdbc.update("DELETE FROM assistant_daily_metrics");
        jdbc.update("DELETE FROM assistant_daily_product_metrics");
    }

    @Test
    void consumesConsumerAndRestaurantQuotasTogether() {
        var result = quotaService.consume(RESTAURANT_ID, "opaque-session-hmac-1");

        assertEquals(19, result.remainingConsumer());
        assertEquals(99, result.remainingRestaurant());
        assertEquals(1, consumed("CONSUMER_SESSION", "opaque-session-hmac-1"));
        assertEquals(1, consumed("RESTAURANT", RESTAURANT_ID.toString()));
    }

    @Test
    void blocksTwentyFirstInteractionForSameConsumerSession() {
        for (int i = 0; i < AssistantQuotaService.CONSUMER_DAILY_LIMIT; i++) {
            quotaService.consume(RESTAURANT_ID, "opaque-session-hmac-2");
        }

        assertThrows(AssistantQuotaService.AssistantQuotaExceededException.class,
                () -> quotaService.consume(RESTAURANT_ID, "opaque-session-hmac-2"));

        assertEquals(20, consumed("CONSUMER_SESSION", "opaque-session-hmac-2"));
        assertEquals(20, consumed("RESTAURANT", RESTAURANT_ID.toString()));
    }

    @Test
    void restaurantLimitRollsBackTheConsumerIncrement() {
        for (int i = 0; i < AssistantQuotaService.RESTAURANT_DAILY_LIMIT; i++) {
            quotaService.consume(RESTAURANT_ID, "unique-session-hmac-" + i);
        }

        String lastConsumerKey = "new-session-at-restaurant-limit";
        assertThrows(AssistantQuotaService.AssistantQuotaExceededException.class,
                () -> quotaService.consume(RESTAURANT_ID, lastConsumerKey));

        assertEquals(0, consumed("CONSUMER_SESSION", lastConsumerKey));
        assertEquals(100, consumed("RESTAURANT", RESTAURANT_ID.toString()));
    }


    @Test
    void concurrentRequestsNeverExceedConsumerQuota() throws Exception {
        int attempts = 50;
        var executor = Executors.newFixedThreadPool(10);
        var start = new CountDownLatch(1);
        try {
            var futures = new ArrayList<Future<Boolean>>();
            for (int i = 0; i < attempts; i++) {
                futures.add(executor.submit(() -> {
                    start.await();
                    try {
                        quotaService.consume(RESTAURANT_ID, "same-concurrent-session");
                        return true;
                    } catch (AssistantQuotaService.AssistantQuotaExceededException expected) {
                        return false;
                    }
                }));
            }
            start.countDown();

            int accepted = 0;
            for (Future<Boolean> future : futures) {
                if (future.get()) accepted++;
            }

            assertEquals(AssistantQuotaService.CONSUMER_DAILY_LIMIT, accepted);
            assertEquals(AssistantQuotaService.CONSUMER_DAILY_LIMIT,
                    consumed("CONSUMER_SESSION", "same-concurrent-session"));
            assertEquals(AssistantQuotaService.CONSUMER_DAILY_LIMIT,
                    consumed("RESTAURANT", RESTAURANT_ID.toString()));
        } finally {
            executor.shutdownNow();
        }
    }


    @Test
    void blocksThirtyFirstRequestPerIpAndStoresOnlyHmac() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.42");

        for (int i = 0; i < AssistantIpRateLimitService.REQUESTS_PER_MINUTE; i++) {
            ipRateLimitService.checkAndConsume(request);
        }

        var exception = assertThrows(AssistantIpRateLimitService.AssistantIpRateLimitExceededException.class,
                () -> ipRateLimitService.checkAndConsume(request));
        assertEquals(true, exception.getRetryAfterSeconds() > 0);

        var hashes = jdbc.query("SELECT ip_hash FROM assistant_ip_rate_limit",
                (resultSet, rowNumber) -> resultSet.getString(1));
        assertEquals(1, hashes.size());
        assertEquals(64, hashes.getFirst().length());
        org.junit.jupiter.api.Assertions.assertNotEquals("203.0.113.42", hashes.getFirst());
    }

    @Test
    void ignoresForwardedForHeaderUnlessExplicitlyTrusted() {
        MockHttpServletRequest first = new MockHttpServletRequest();
        first.setRemoteAddr("10.0.0.8");
        first.addHeader("X-Forwarded-For", "198.51.100.10");

        MockHttpServletRequest second = new MockHttpServletRequest();
        second.setRemoteAddr("10.0.0.8");
        second.addHeader("X-Forwarded-For", "198.51.100.11");

        ipRateLimitService.checkAndConsume(first);
        ipRateLimitService.checkAndConsume(second);

        var rows = jdbc.query("SELECT request_count FROM assistant_ip_rate_limit",
                (resultSet, rowNumber) -> resultSet.getInt(1));
        assertEquals(1, rows.size());
        assertEquals(2, rows.getFirst());
    }



    @Test
    void purgesPseudonymousIpBucketsOlderThanTwoHours() {
        jdbc.update("""
                INSERT INTO assistant_ip_rate_limit (window_start, ip_hash, request_count)
                VALUES (?, ?, 1)
                """, java.sql.Timestamp.from(java.time.Instant.now().minus(java.time.Duration.ofHours(3))),
                "a".repeat(64));

        ipRateLimitService.purgeExpiredBuckets();

        Integer rows = jdbc.queryForObject("SELECT COUNT(*) FROM assistant_ip_rate_limit", Integer.class);
        assertEquals(0, rows);
    }

    @Test
    void aggregatesOutcomesTokensRecommendationsAndClicksWithoutSessionData() {
        metricsService.recordInteractionOutcome(
                RESTAURANT_ID, 145, true, false, java.util.List.of(PRODUCT_ID), 120, 45
        );
        metricsService.recordProviderError(RESTAURANT_ID);
        metricsService.recordProductClick(RESTAURANT_ID, PRODUCT_ID);

        assertEquals(1, metric("INTERACTIONS"));
        assertEquals(1, metric("FALLBACKS"));
        assertEquals(145, metric("LATENCY_TOTAL_MS"));
        assertEquals(120, metric("INPUT_TOKENS"));
        assertEquals(45, metric("OUTPUT_TOKENS"));
        assertEquals(1, metric("PROVIDER_ERRORS"));
        assertEquals(149, metric("ESTIMATED_COST_MICRO_USD"));

        var productCounts = jdbc.query("""
                SELECT recommendation_count, click_count
                FROM assistant_daily_product_metrics
                WHERE metric_date = (CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo')::date
                  AND restaurant_id = ?
                  AND product_id = ?
                """, (rs, row) -> new long[]{rs.getLong(1), rs.getLong(2)}, RESTAURANT_ID, PRODUCT_ID);
        assertEquals(1, productCounts.size());
        assertEquals(1, productCounts.getFirst()[0]);
        assertEquals(1, productCounts.getFirst()[1]);
    }

    @Test
    void purgesAggregatedMetricsOlderThanRetentionWindowOnNextWrite() {
        jdbc.update("""
                INSERT INTO assistant_daily_metrics (metric_date, restaurant_id, metric_key, metric_value)
                VALUES (?, ?, 'INTERACTIONS', 9)
                """, java.time.LocalDate.now(AssistantQuotaService.BUSINESS_ZONE)
                .minusDays(AssistantMetricsService.RETENTION_DAYS + 1), RESTAURANT_ID);

        metricsService.recordInteractionOutcome(RESTAURANT_ID, 10, false, false, java.util.List.of(), 0, 0);

        Integer oldRows = jdbc.queryForObject("""
                SELECT COUNT(*) FROM assistant_daily_metrics
                WHERE metric_date < ?
                """, Integer.class, java.time.LocalDate.now(AssistantQuotaService.BUSINESS_ZONE)
                .minusDays(AssistantMetricsService.RETENTION_DAYS));
        assertEquals(0, oldRows);
    }

    private long metric(String key) {
        Long value = jdbc.queryForObject("""
                SELECT metric_value
                FROM assistant_daily_metrics
                WHERE metric_date = (CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo')::date
                  AND restaurant_id = ?
                  AND metric_key = ?
                """, Long.class, RESTAURANT_ID, key);
        return value == null ? 0 : value;
    }

    private int consumed(String scopeType, String scopeKey) {
        var values = jdbc.query("""
                SELECT consumed
                FROM assistant_daily_quota
                WHERE quota_date = (CURRENT_TIMESTAMP AT TIME ZONE 'America/Sao_Paulo')::date
                  AND scope_type = ?
                  AND scope_key = ?
                """, (resultSet, rowNumber) -> resultSet.getInt(1), scopeType, scopeKey);
        return values.isEmpty() ? 0 : values.getFirst();
    }
}
