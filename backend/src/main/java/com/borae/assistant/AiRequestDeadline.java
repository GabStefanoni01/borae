package com.borae.assistant;

import java.util.function.LongSupplier;

/**
 * Deadline absoluto compartilhado por toda uma solicitação ao Assistente Gastronômico
 * (incluindo tentativa primária no Gemini e eventual contingência no Grok).
 *
 * <p>Garante que o timeout efetivo de cada chamada HTTP nunca exceda o tempo restante
 * do orçamento global ({@code AI_TOTAL_TIMEOUT_MS}).</p>
 */
public final class AiRequestDeadline {

    public static final long DEFAULT_MIN_OPERATIONAL_MS = 500L;

    private final long startedAtMs;
    private final long totalBudgetMs;
    private final long deadlineEpochMs;
    private final long minOperationalMs;
    private final LongSupplier clock;

    private AiRequestDeadline(long startedAtMs, long totalBudgetMs, long minOperationalMs, LongSupplier clock) {
        this.clock = clock == null ? System::currentTimeMillis : clock;
        this.startedAtMs = startedAtMs;
        this.totalBudgetMs = Math.max(1L, totalBudgetMs);
        this.deadlineEpochMs = startedAtMs + this.totalBudgetMs;
        this.minOperationalMs = Math.max(1L, minOperationalMs);
    }

    public static AiRequestDeadline start(long totalBudgetMs) {
        return start(totalBudgetMs, DEFAULT_MIN_OPERATIONAL_MS, System::currentTimeMillis);
    }

    public static AiRequestDeadline start(long totalBudgetMs, LongSupplier clock) {
        return start(totalBudgetMs, DEFAULT_MIN_OPERATIONAL_MS, clock);
    }

    public static AiRequestDeadline start(long totalBudgetMs, long minOperationalMs, LongSupplier clock) {
        LongSupplier effectiveClock = clock == null ? System::currentTimeMillis : clock;
        return new AiRequestDeadline(effectiveClock.getAsLong(), totalBudgetMs, minOperationalMs, effectiveClock);
    }

    public long totalBudgetMs() {
        return totalBudgetMs;
    }

    public long elapsedMillis() {
        return Math.max(0L, clock.getAsLong() - startedAtMs);
    }

    public long remainingMillis() {
        return Math.max(0L, deadlineEpochMs - clock.getAsLong());
    }

    public boolean isExpired() {
        return remainingMillis() <= 0L;
    }

    public boolean hasOperationalBudget() {
        return remainingMillis() >= minOperationalMs;
    }

    /**
     * Retorna o menor valor entre o timeout individual configurado do provedor e o tempo
     * restante do deadline global. Retorna {@code 0} quando o deadline já expirou.
     */
    public long timeoutForProvider(long configuredProviderTimeoutMs) {
        long remaining = remainingMillis();
        if (remaining <= 0L) {
            return 0L;
        }
        long safeConfigured = Math.max(1L, configuredProviderTimeoutMs);
        return Math.min(safeConfigured, remaining);
    }
}
