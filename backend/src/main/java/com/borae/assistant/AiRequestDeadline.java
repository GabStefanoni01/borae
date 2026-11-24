package com.borae.assistant;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Deadline absoluto monotônico compartilhado por toda uma solicitação ao Assistente Gastronômico
 * (tentativa primária no Gemini e eventual contingência no Grok).
 *
 * <p>Baseado em {@link System#nanoTime()} (relógio monotônico imune a ajustes de relógio de parede
 * ou sincronização NTP), com cálculo protegido contra overflow e valores negativos.</p>
 */
public final class AiRequestDeadline {

    public static final long DEFAULT_MIN_OPERATIONAL_MS = 500L;
    private static final long MAX_SAFE_BUDGET_MS = Long.MAX_VALUE / 1_000_000L;

    private final long startedAtNanos;
    private final long totalBudgetMs;
    private final long totalBudgetNanos;
    private final long minOperationalNanos;
    private final LongSupplier nanoClock;

    private AiRequestDeadline(long startedAtNanos, long totalBudgetMs, long minOperationalMs, LongSupplier nanoClock) {
        this.nanoClock = nanoClock == null ? System::nanoTime : nanoClock;
        this.startedAtNanos = startedAtNanos;
        long clampedBudgetMs = Math.max(1L, Math.min(totalBudgetMs, MAX_SAFE_BUDGET_MS));
        long clampedMinMs = Math.max(1L, Math.min(minOperationalMs, MAX_SAFE_BUDGET_MS));
        this.totalBudgetMs = clampedBudgetMs;
        this.totalBudgetNanos = TimeUnit.MILLISECONDS.toNanos(clampedBudgetMs);
        this.minOperationalNanos = TimeUnit.MILLISECONDS.toNanos(clampedMinMs);
    }

    public static AiRequestDeadline start(long totalBudgetMs) {
        return start(totalBudgetMs, DEFAULT_MIN_OPERATIONAL_MS, System::nanoTime);
    }

    public static AiRequestDeadline start(long totalBudgetMs, LongSupplier nanoClock) {
        return start(totalBudgetMs, DEFAULT_MIN_OPERATIONAL_MS, nanoClock);
    }

    public static AiRequestDeadline start(long totalBudgetMs, long minOperationalMs, LongSupplier nanoClock) {
        LongSupplier effectiveClock = nanoClock == null ? System::nanoTime : nanoClock;
        return new AiRequestDeadline(effectiveClock.getAsLong(), totalBudgetMs, minOperationalMs, effectiveClock);
    }

    public long totalBudgetMs() {
        return totalBudgetMs;
    }

    public long elapsedNanos() {
        long delta = nanoClock.getAsLong() - startedAtNanos;
        return Math.max(0L, delta);
    }

    public long elapsedMillis() {
        return TimeUnit.NANOSECONDS.toMillis(elapsedNanos());
    }

    public long remainingNanos() {
        long remaining = totalBudgetNanos - elapsedNanos();
        return Math.max(0L, remaining);
    }

    public long remainingMillis() {
        return TimeUnit.NANOSECONDS.toMillis(remainingNanos());
    }

    public boolean isExpired() {
        return remainingNanos() <= 0L;
    }

    public boolean hasOperationalBudget() {
        return remainingNanos() >= minOperationalNanos;
    }

    /**
     * Retorna o menor valor entre o timeout individual configurado do provedor e o tempo
     * restante do deadline monotônico global. Retorna {@code 0} quando o deadline já expirou.
     */
    public long timeoutForProvider(long configuredProviderTimeoutMs) {
        long remainingNanos = remainingNanos();
        if (remainingNanos <= 0L) {
            return 0L;
        }
        long remainingMs = Math.max(1L, TimeUnit.NANOSECONDS.toMillis(remainingNanos));
        long safeConfigured = Math.max(1L, configuredProviderTimeoutMs);
        return Math.min(safeConfigured, remainingMs);
    }
}
