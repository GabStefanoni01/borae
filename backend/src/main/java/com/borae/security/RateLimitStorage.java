package com.borae.security;

import java.util.Optional;

/**
 * Abstração de armazenamento de contadores de rate limit, adaptada de
 * {@code ArmazenamentoDeLimite} em {@code src/limite-compartilhado/armazenamento.ts} do Neves Shield.
 */
public interface RateLimitStorage {

    String name();

    boolean shared();

    RateLimitWindow increment(String key, long windowMs);

    default Optional<RateLimitWindow> getActiveWindow(String key) {
        return Optional.empty();
    }

    default void remove(String key) {
    }

    void reset();
}
