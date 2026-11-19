package com.borae.security;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.transaction.TransactionUsageException;

import java.sql.SQLException;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Armazenamento resiliente que combina o driver primário (PostgreSQL distribuído)
 * com reserva local em memória ({@code comReservaLocal} em {@code src/limite-compartilhado/armazenamento.ts}
 * do Neves Shield).
 *
 * <p>Se o banco primário falhar ou não estiver disponível, degrada para contagem por instância
 * registrando alerta no canal de segurança com throttling (no máximo 1 log detalhado a cada 60 segundos)
 * para evitar inundação de logs sob falha persistente, e registra a recuperação quando o banco volta.</p>
 *
 * <p>Erros de programação ou de configuração transacional (ex.: {@link IllegalArgumentException},
 * {@link TransactionUsageException}, {@link InvalidDataAccessApiUsageException} ou tentativa de escrita
 * em transação {@code read-only} SQLState {@code 25006}) não são mascarados pelo fallback e são
 * propagados imediatamente.</p>
 */
public class ResilientRateLimitStorage implements RateLimitStorage {

    private static final long LOG_THROTTLE_WINDOW_MS = 60_000L;
    private static final String POSTGRES_READ_ONLY_TX_SQL_STATE = "25006";

    private final RateLimitStorage primary;
    private final RateLimitStorage fallback;
    private final SecurityAuditLogger auditLogger;
    private final LongSupplier clock;

    private final AtomicLong lastWarningLoggedAt = new AtomicLong(0L);
    private final AtomicLong suppressedWarnings = new AtomicLong(0L);
    private final AtomicBoolean degraded = new AtomicBoolean(false);

    public ResilientRateLimitStorage(
            RateLimitStorage primary,
            RateLimitStorage fallback,
            SecurityAuditLogger auditLogger
    ) {
        this(primary, fallback, auditLogger, System::currentTimeMillis);
    }

    public ResilientRateLimitStorage(
            RateLimitStorage primary,
            RateLimitStorage fallback,
            SecurityAuditLogger auditLogger,
            LongSupplier clock
    ) {
        this.primary = primary;
        this.fallback = fallback;
        this.auditLogger = auditLogger;
        this.clock = clock;
    }

    @Override
    public String name() {
        return primary.name() + "+reserva(" + fallback.name() + ")";
    }

    @Override
    public boolean shared() {
        return primary.shared();
    }

    @Override
    public RateLimitWindow increment(String key, long windowMs) {
        try {
            RateLimitWindow result = primary.increment(key, windowMs);
            recordRecoveryIfNeeded();
            return result;
        } catch (RuntimeException exception) {
            rethrowIfConfigurationOrTransactionalError(exception);
            logDegradationThrottled(exception);
            return fallback.increment(key, windowMs);
        }
    }

    @Override
    public Optional<RateLimitWindow> getActiveWindow(String key) {
        try {
            Optional<RateLimitWindow> result = primary.getActiveWindow(key);
            recordRecoveryIfNeeded();
            if (result.isPresent()) {
                return result;
            }
            return fallback.getActiveWindow(key);
        } catch (RuntimeException exception) {
            rethrowIfConfigurationOrTransactionalError(exception);
            logDegradationThrottled(exception);
            return fallback.getActiveWindow(key);
        }
    }

    @Override
    public void remove(String key) {
        try {
            primary.remove(key);
            recordRecoveryIfNeeded();
        } catch (RuntimeException exception) {
            rethrowIfConfigurationOrTransactionalError(exception);
            logDegradationThrottled(exception);
        } finally {
            fallback.remove(key);
        }
    }

    @Override
    public void reset() {
        try {
            primary.reset();
        } catch (RuntimeException ignored) {
            // Ignora falha no primário durante reset de testes.
        } finally {
            fallback.reset();
            lastWarningLoggedAt.set(0L);
            suppressedWarnings.set(0L);
            degraded.set(false);
        }
    }

    public boolean isDegraded() {
        return degraded.get();
    }

    private static void rethrowIfConfigurationOrTransactionalError(RuntimeException exception) {
        if (exception instanceof IllegalArgumentException
                || exception instanceof TransactionUsageException
                || exception instanceof InvalidDataAccessApiUsageException) {
            throw exception;
        }
        Throwable cause = exception;
        int depth = 0;
        while (cause != null && depth < 10) {
            if (cause instanceof SQLException sqlException
                    && POSTGRES_READ_ONLY_TX_SQL_STATE.equals(sqlException.getSQLState())) {
                throw exception;
            }
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("read-only transaction")) {
                throw exception;
            }
            cause = cause.getCause();
            depth++;
        }
    }

    private void logDegradationThrottled(RuntimeException exception) {
        degraded.set(true);
        if (auditLogger == null) {
            return;
        }
        long now = clock.getAsLong();
        long previous = lastWarningLoggedAt.get();
        if (previous == 0L || (now - previous) >= LOG_THROTTLE_WINDOW_MS) {
            if (lastWarningLoggedAt.compareAndSet(previous, now)) {
                long suppressed = suppressedWarnings.getAndSet(0L);
                String extra = suppressed > 0 ? " (falhas suprimidas no intervalo=" + suppressed + ")" : "";
                auditLogger.logSecurityEvent(
                        "ALTA",
                        "rate_limit_fallback",
                        "INTERNAL",
                        "rate-limit-storage",
                        "n/a",
                        false,
                        "Armazenamento \"" + primary.name() + "\" indisponível; contando em \""
                                + fallback.name() + "\"" + extra + " :: " + exception.getClass().getSimpleName()
                );
                return;
            }
        }
        suppressedWarnings.incrementAndGet();
    }

    private void recordRecoveryIfNeeded() {
        if (degraded.compareAndSet(true, false) && auditLogger != null) {
            long suppressed = suppressedWarnings.getAndSet(0L);
            auditLogger.logSecurityEvent(
                    "MEDIA",
                    "rate_limit_recovered",
                    "INTERNAL",
                    "rate-limit-storage",
                    "n/a",
                    false,
                    "Armazenamento primário \"" + primary.name() + "\" restabelecido (eventos em fallback suprimidos=" + suppressed + ")."
            );
        }
    }
}
