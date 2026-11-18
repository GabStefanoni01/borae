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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@JdbcTest
@Import(AssistantQuotaService.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class AssistantQuotaPostgresIntegrationTest {

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
    }

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AssistantQuotaService quotaService;

    @BeforeEach
    void cleanCounters() {
        jdbc.update("DELETE FROM assistant_daily_quota");
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
