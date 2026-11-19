package com.borae.security;

/**
 * Armazenamento resiliente que combina o driver primário (PostgreSQL distribuído)
 * com reserva local em memória ({@code comReservaLocal} em {@code src/limite-compartilhado/armazenamento.ts}
 * do Neves Shield).
 *
 * <p>Se o banco primário falhar ou não estiver disponível, degrada para contagem por instância
 * registrando alerta no canal de segurança, nunca liberando requisições silenciosamente.</p>
 */
public class ResilientRateLimitStorage implements RateLimitStorage {

    private final RateLimitStorage primary;
    private final RateLimitStorage fallback;
    private final SecurityAuditLogger auditLogger;

    public ResilientRateLimitStorage(
            RateLimitStorage primary,
            RateLimitStorage fallback,
            SecurityAuditLogger auditLogger
    ) {
        this.primary = primary;
        this.fallback = fallback;
        this.auditLogger = auditLogger;
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
            return primary.increment(key, windowMs);
        } catch (RuntimeException exception) {
            if (auditLogger != null) {
                auditLogger.logSecurityEvent(
                        "ALTA",
                        "rate_limit_fallback",
                        "INTERNAL",
                        "rate-limit-storage",
                        "n/a",
                        false,
                        "Armazenamento \"" + primary.name() + "\" indisponível; contando em \""
                                + fallback.name() + "\" :: " + exception.getClass().getSimpleName()
                );
            }
            return fallback.increment(key, windowMs);
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
        }
    }
}
