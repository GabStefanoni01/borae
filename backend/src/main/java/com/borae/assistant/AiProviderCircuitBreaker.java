package com.borae.assistant;

import java.util.function.LongSupplier;

/**
 * Circuit breaker leve e independente por provedor de IA.
 * Evita latência em cascata quando um provedor apresenta falhas técnicas consecutivas,
 * sem afetar o estado do outro provedor e sem abrir em recusas de segurança ou erro de entrada.
 */
public class AiProviderCircuitBreaker {

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    private final AiProviderId providerId;
    private final int failureThreshold;
    private final long openDurationMs;
    private final LongSupplier clock;

    private State state = State.CLOSED;
    private int consecutiveFailures = 0;
    private long openedAtMs = 0L;
    private boolean halfOpenProbeInFlight = false;

    public AiProviderCircuitBreaker(
            AiProviderId providerId,
            int failureThreshold,
            long openDurationMs,
            LongSupplier clock
    ) {
        this.providerId = providerId;
        this.failureThreshold = Math.max(1, failureThreshold);
        this.openDurationMs = Math.max(1_000L, openDurationMs);
        this.clock = clock == null ? System::currentTimeMillis : clock;
    }

    public AiProviderId providerId() {
        return providerId;
    }

    public synchronized boolean allowRequest() {
        if (state == State.CLOSED) {
            return true;
        }
        long now = clock.getAsLong();
        if (state == State.OPEN) {
            if (now - openedAtMs >= openDurationMs) {
                state = State.HALF_OPEN;
                halfOpenProbeInFlight = true;
                return true;
            }
            return false;
        }
        // HALF_OPEN: permite apenas uma chamada de sondagem por vez
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
            openedAtMs = clock.getAsLong();
            halfOpenProbeInFlight = false;
            consecutiveFailures = failureThreshold;
            return;
        }
        consecutiveFailures++;
        if (consecutiveFailures >= failureThreshold) {
            state = State.OPEN;
            openedAtMs = clock.getAsLong();
        }
    }

    public synchronized State state() {
        if (state == State.OPEN && clock.getAsLong() - openedAtMs >= openDurationMs) {
            return State.HALF_OPEN;
        }
        return state;
    }

    public synchronized void reset() {
        state = State.CLOSED;
        consecutiveFailures = 0;
        openedAtMs = 0L;
        halfOpenProbeInFlight = false;
    }
}
