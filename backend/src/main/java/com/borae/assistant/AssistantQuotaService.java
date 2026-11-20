package com.borae.assistant;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Atomically consumes both daily quotas in one database transaction.
 * If either scope is exhausted, the exception rolls back the other scope's increment.
 */
@Service
public class AssistantQuotaService {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    public static final int CONSUMER_DAILY_LIMIT = 20;
    public static final int RESTAURANT_DAILY_LIMIT = 100;

    private static final String CONSUME_SQL = """
            INSERT INTO assistant_daily_quota (quota_date, scope_type, scope_key, consumed, updated_at)
            VALUES (?, ?, ?, 1, NOW())
            ON CONFLICT (quota_date, scope_type, scope_key)
            DO UPDATE SET consumed = assistant_daily_quota.consumed + 1, updated_at = NOW()
            WHERE assistant_daily_quota.consumed < ?
            RETURNING consumed
            """;

    private final JdbcTemplate jdbcTemplate;

    public AssistantQuotaService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public QuotaConsumption consume(UUID restaurantId, String consumerQuotaKey) {
        if (restaurantId == null) {
            throw new IllegalArgumentException("O restaurante é obrigatório para consumir a cota.");
        }
        if (consumerQuotaKey == null || consumerQuotaKey.isBlank() || consumerQuotaKey.length() > 64) {
            throw new IllegalArgumentException("A sessão anônima válida é obrigatória para consumir a cota.");
        }

        LocalDate quotaDate = LocalDate.now(BUSINESS_ZONE);
        int consumerConsumed = consumeScope(quotaDate, "CONSUMER_SESSION", consumerQuotaKey,
                CONSUMER_DAILY_LIMIT, QuotaScope.CONSUMER);
        int restaurantConsumed = consumeScope(quotaDate, "RESTAURANT", restaurantId.toString(),
                RESTAURANT_DAILY_LIMIT, QuotaScope.RESTAURANT);

        ZonedDateTime resetsAt = quotaDate.plusDays(1).atStartOfDay(BUSINESS_ZONE);
        return new QuotaConsumption(
                CONSUMER_DAILY_LIMIT - consumerConsumed,
                RESTAURANT_DAILY_LIMIT - restaurantConsumed,
                resetsAt
        );
    }

    private int consumeScope(LocalDate date, String scopeType, String scopeKey,
                             int limit, QuotaScope scope) {
        List<Integer> consumed = jdbcTemplate.query(
                CONSUME_SQL,
                (resultSet, rowNumber) -> resultSet.getInt(1),
                date, scopeType, scopeKey, limit
        );
        if (consumed.isEmpty()) {
            throw new AssistantQuotaExceededException(scope);
        }
        return consumed.getFirst();
    }

    public enum QuotaScope {
        CONSUMER, RESTAURANT
    }

    public record QuotaConsumption(int remainingConsumer, int remainingRestaurant,
                                   ZonedDateTime resetsAt) {}

    public static class AssistantQuotaExceededException extends RuntimeException {
        private final QuotaScope scope;

        public AssistantQuotaExceededException(QuotaScope scope) {
            super("A cota diária do assistente foi atingida.");
            this.scope = scope;
        }

        public QuotaScope getScope() {
            return scope;
        }
    }
}
