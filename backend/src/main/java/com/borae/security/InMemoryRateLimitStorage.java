package com.borae.security;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Armazenamento em memória com limpeza periódica de janelas expiradas,
 * adaptado de {@code criarArmazenamentoEmMemoria} em {@code src/limite-compartilhado/armazenamento.ts} do Neves Shield.
 */
public class InMemoryRateLimitStorage implements RateLimitStorage {

    private static final long CLEANUP_INTERVAL_MS = 60_000L;

    private final LongSupplier clock;
    private final Map<String, RateLimitWindow> windows = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong();

    public InMemoryRateLimitStorage() {
        this(System::currentTimeMillis);
    }

    public InMemoryRateLimitStorage(LongSupplier clock) {
        this.clock = clock;
        this.lastCleanup.set(clock.getAsLong());
    }

    @Override
    public String name() {
        return "memoria";
    }

    @Override
    public boolean shared() {
        return false;
    }

    @Override
    public RateLimitWindow increment(String key, long windowMs) {
        long now = clock.getAsLong();
        cleanupIfDue(now);
        return windows.compute(key, (k, existing) -> {
            if (existing == null || now >= existing.resetAtEpochMs()) {
                return new RateLimitWindow(1, now + windowMs);
            }
            return new RateLimitWindow(existing.count() + 1, existing.resetAtEpochMs());
        });
    }

    @Override
    public Optional<RateLimitWindow> getActiveWindow(String key) {
        if (key == null) {
            return Optional.empty();
        }
        long now = clock.getAsLong();
        cleanupIfDue(now);
        RateLimitWindow existing = windows.get(key);
        if (existing == null) {
            return Optional.empty();
        }
        if (now >= existing.resetAtEpochMs()) {
            windows.remove(key, existing);
            return Optional.empty();
        }
        return Optional.of(existing);
    }

    @Override
    public void remove(String key) {
        if (key != null) {
            windows.remove(key);
        }
    }

    @Override
    public void reset() {
        windows.clear();
    }

    private void cleanupIfDue(long now) {
        long previous = lastCleanup.get();
        if (now - previous > CLEANUP_INTERVAL_MS && lastCleanup.compareAndSet(previous, now)) {
            windows.entrySet().removeIf(entry -> now >= entry.getValue().resetAtEpochMs());
        }
    }
}
