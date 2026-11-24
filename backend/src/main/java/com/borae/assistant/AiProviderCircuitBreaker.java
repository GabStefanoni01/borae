package com.borae.assistant;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Circuit breaker leve e independente por provedor de IA, baseado em relógio monotônico
 * ({@link System#nanoTime()}). Evita latência em cascata quando um provedor apresenta falhas
 * técnicas consecutivas, sem afetar o estado do outro provedor e sem abrir em recusas de segurança.
 */
public class AiProviderCircuitBreaker {

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private static final long MAX_SAFE_DURATION_MS = Long.MAX_VALUE / 1_000_000L;

    private final AiProviderId providerId;
    private final int failureThreshold;
    private final long openDurationNanos;
    private final LongSupplier nanoClock;

    private State state = State.CLOSED;
    private int consecutiveFailures = 0;
    private long openedAtNanos = 0L;
    private boolean halfOpenProbeInFlight = false;

    public AiProviderCircuitBreaker(
            AiProviderId providerId,
            int failureThreshold,
            long openDurationMs,
            LongSupplier nanoClock
    ) {
        this.providerId = providerId;
        this.failureThreshold = Math.max(1, failureThreshold);
        long safeOpenMs = Math.max(1_000L, Math.min(openDurationMs, MAX_SAFE_DURATION_MS));
        this.openDurationNanos = TimeUnit.MILLISECONDS.toNanos(safeOpenMs);
        this.nanoClock = nanoClock == null ? System::nanoTime : nanoClock;
    }

    public AiProviderId providerId() {
        return providerId;
    }

    public synchronized boolean allowRequest() {
        if (state == State.CLOSED) {
            return true;
        }
        if (state == State.OPEN) {
            if (elapsedSinceOpenedNanos() >= openDurationNanos) {
                state = State.HALF_OPEN;
                halfOpenProbeInFlight = true;
                return true;
            }
            return false;
        }
        if (!halfOpenProbeInFlight) {
            halfOpenProbeInFlight = true;
            return true;
        }
        return false;
    }

    public synchronized void recordSuccess() {
        consecutiveFailures = 0;
        halfOpenProbeInFlight = false;
        state = State.CLOSED;
    }

    public synchronized void recordFailure(AiProviderFailure.FailureType type) {
        if (type == null || !type.tripsCircuitBreaker()) {
            if (state == State.HALF_OPEN) {
                halfOpenProbeInFlight = false;
            }
            return;
        }
        if (state == State.HALF_OPEN) {
            state = State.OPEN;
            openedAtNanos = nanoClock.getAsLong();
            halfOpenProbeInFlight = false;
            consecutiveFailures = failureThreshold;
            return;
        }
        consecutiveFailures++;
        if (consecutiveFailures >= failureThreshold) {
            state = State.OPEN;
            openedAtNanos = nanoClock.getAsLong();
        }
    }

    public synchronized State state() {
        if (state == State.OPEN && elapsedSinceOpenedNanos() >= openDurationNanos) {
            return State.HALF_OPEN;
        }
        return state;
    }

    public synchronized void reset() {
        state = State.CLOSED;
        consecutiveFailures = 0;
        openedAtNanos = 0L;
        halfOpenProbeInFlight = false;
    }

    private long elapsedSinceOpenedNanos() {
        return Math.max(0L, nanoClock.getAsLong() - openedAtNanos);
    }
}
