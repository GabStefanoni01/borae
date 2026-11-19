package com.borae.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/**
 * Armazenamento distribuído de rate limit no PostgreSQL via UPSERT atômico,
 * adaptado de {@code sql/limites_de_uso.sql} e {@code src/limite-compartilhado/postgres.ts} do Neves Shield.
 *
 * <p>Executa cada incremento, consulta e remoção em uma transação independente de leitura e escrita
 * ({@code PROPAGATION_REQUIRES_NEW}, {@code readOnly = false}), garantindo que o contador seja
 * imediatamente confirmado no PostgreSQL e nunca sofra rollback ou bloqueio por transações
 * externas {@code readOnly} ou exceções de autenticação (HTTP 401).</p>
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

    private static final String SELECT_ACTIVE_SQL = """
            SELECT request_count, reset_at
            FROM security_rate_limit
            WHERE bucket_key = ?
              AND reset_at > CURRENT_TIMESTAMP
            """;

    private static final String DELETE_KEY_SQL = """
            DELETE FROM security_rate_limit
            WHERE bucket_key = ?
            """;

    private static final String CLEANUP_EXPIRED_BATCH_SQL = """
            DELETE FROM security_rate_limit
            WHERE bucket_key IN (
                SELECT bucket_key
                FROM security_rate_limit
                WHERE reset_at < CURRENT_TIMESTAMP - INTERVAL '15 minutes'
                LIMIT 500
            )
            """;

    private static final long CLEANUP_INTERVAL_MS = 10 * 60 * 1000L;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate requiresNewTransactionTemplate;
    private final AtomicLong lastCleanup = new AtomicLong(System.currentTimeMillis());

    public PostgresRateLimitStorage(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, null);
    }

    public PostgresRateLimitStorage(JdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = jdbcTemplate;
        PlatformTransactionManager effectiveTransactionManager = transactionManager;
        if (effectiveTransactionManager == null && jdbcTemplate != null && jdbcTemplate.getDataSource() != null) {
            effectiveTransactionManager = new DataSourceTransactionManager(jdbcTemplate.getDataSource());
        }
        if (effectiveTransactionManager != null) {
            TransactionTemplate template = new TransactionTemplate(effectiveTransactionManager);
            template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            template.setReadOnly(false);
            this.requiresNewTransactionTemplate = template;
        } else {
            this.requiresNewTransactionTemplate = null;
        }
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
        validateKey(key);
        if (windowMs < 1000L || windowMs > 604_800_000L) {
            throw new IllegalArgumentException("Janela de rate limit inválida.");
        }
        return executeInIndependentTransaction(() -> {
            RateLimitWindow window = jdbcTemplate.queryForObject(
                    ATOMIC_UPSERT_SQL,
                    (rs, rowNum) -> {
                        int count = rs.getInt("request_count");
                        Timestamp resetAt = rs.getTimestamp("reset_at");
                        return new RateLimitWindow(count, resetAt.toInstant().toEpochMilli());
                    },
                    key,
                    windowMs,
                    windowMs
            );
            if (window == null) {
                throw new IllegalStateException("Resposta nula ao atualizar contador de rate limit no PostgreSQL.");
            }
            cleanupExpiredIfDue();
            return window;
        });
    }

    @Override
    public Optional<RateLimitWindow> getActiveWindow(String key) {
        validateKey(key);
        return executeInIndependentTransaction(() -> {
            List<RateLimitWindow> rows = jdbcTemplate.query(
                    SELECT_ACTIVE_SQL,
                    (rs, rowNum) -> new RateLimitWindow(
                            rs.getInt("request_count"),
                            rs.getTimestamp("reset_at").toInstant().toEpochMilli()
                    ),
                    key
            );
            return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
        });
    }

    @Override
    public void remove(String key) {
        if (key == null || key.length() < 3 || key.length() > 200) {
            return;
        }
        executeInIndependentTransaction(() -> {
            jdbcTemplate.update(DELETE_KEY_SQL, key);
            return null;
        });
    }

    @Override
    public void reset() {
        executeInIndependentTransaction(() -> {
            jdbcTemplate.update("DELETE FROM security_rate_limit");
            return null;
        });
    }

    private <T> T executeInIndependentTransaction(Supplier<T> action) {
        if (requiresNewTransactionTemplate != null) {
            return requiresNewTransactionTemplate.execute(status -> action.get());
        }
        return action.get();
    }

    private static void validateKey(String key) {
        if (key == null || key.length() < 3 || key.length() > 200) {
            throw new IllegalArgumentException("Chave de rate limit inválida.");
        }
    }

    private void cleanupExpiredIfDue() {
        long now = System.currentTimeMillis();
        long previous = lastCleanup.get();
        if (now - previous > CLEANUP_INTERVAL_MS && lastCleanup.compareAndSet(previous, now)) {
            try {
                jdbcTemplate.update(CLEANUP_EXPIRED_BATCH_SQL);
            } catch (RuntimeException ignored) {
                // Limpeza oportunista não deve interromper a requisição atual.
            }
        }
    }
}
