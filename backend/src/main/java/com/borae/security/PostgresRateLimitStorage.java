package com.borae.security;

import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Armazenamento distribuído de rate limit no PostgreSQL via UPSERT atômico,
 * adaptado de {@code sql/limites_de_uso.sql} e {@code src/limite-compartilhado/postgres.ts} do Neves Shield.
 *
 * <p>Garante que múltiplas instâncias do Boraê no Render enxerguem a mesma contagem
 * sem exigir Redis externo no MVP.</p>
 */
public class PostgresRateLimitStorage implements RateLimitStorage {

    private static final String ATOMIC_UPSERT_SQL = """
            INSERT INTO security_rate_limit (bucket_key, request_count, reset_at, updated_at)
            VALUES (?, 1, CURRENT_TIMESTAMP + (? * INTERVAL '1 millisecond'), CURRENT_TIMESTAMP)
            ON CONFLICT (bucket_key) DO UPDATE SET
                request_count = CASE
                    WHEN security_rate_limit.reset_at <= CURRENT_TIMESTAMP THEN 1
                    ELSE security_rate_limit.request_count + 1
                END,
                reset_at = CASE
                    WHEN security_rate_limit.reset_at <= CURRENT_TIMESTAMP
                    THEN CURRENT_TIMESTAMP + (? * INTERVAL '1 millisecond')
                    ELSE security_rate_limit.reset_at
                END,
                updated_at = CURRENT_TIMESTAMP
            RETURNING request_count, reset_at
            """;

    private static final String CLEANUP_EXPIRED_SQL = """
            DELETE FROM security_rate_limit
            WHERE reset_at < CURRENT_TIMESTAMP - INTERVAL '1 hour'
            """;

    private static final long CLEANUP_INTERVAL_MS = 10 * 60 * 1000L;

    private final JdbcTemplate jdbcTemplate;
    private final AtomicLong lastCleanup = new AtomicLong(System.currentTimeMillis());

    public PostgresRateLimitStorage(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String name() {
        return "postgres";
    }

    @Override
    public boolean shared() {
        return true;
    }

    @Override
    public RateLimitWindow increment(String key, long windowMs) {
        if (key == null || key.length() < 3 || key.length() > 200) {
            throw new IllegalArgumentException("Chave de rate limit inválida.");
        }
        if (windowMs < 1000L || windowMs > 604_800_000L) {
            throw new IllegalArgumentException("Janela de rate limit inválida.");
        }
        String normalizedKey = key;
        RateLimitWindow window = jdbcTemplate.queryForObject(
                ATOMIC_UPSERT_SQL,
                (rs, rowNum) -> {
                    int count = rs.getInt("request_count");
                    Timestamp resetAt = rs.getTimestamp("reset_at");
                    return new RateLimitWindow(count, resetAt.toInstant().toEpochMilli());
                },
                normalizedKey,
                windowMs,
                windowMs
        );
        if (window == null) {
            throw new IllegalStateException("Resposta nula ao atualizar contador de rate limit no PostgreSQL.");
        }
        cleanupExpiredIfDue();
        return window;
    }

    @Override
    public void reset() {
        jdbcTemplate.update("DELETE FROM security_rate_limit");
    }

    private void cleanupExpiredIfDue() {
        long now = System.currentTimeMillis();
        long previous = lastCleanup.get();
        if (now - previous > CLEANUP_INTERVAL_MS && lastCleanup.compareAndSet(previous, now)) {
            try {
                jdbcTemplate.update(CLEANUP_EXPIRED_SQL);
            } catch (RuntimeException ignored) {
                // Limpeza oportunista não deve interromper a requisição atual.
            }
        }
    }
}
