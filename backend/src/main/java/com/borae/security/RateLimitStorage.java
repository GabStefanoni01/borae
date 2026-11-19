package com.borae.security;

/**
 * Abstração de armazenamento de contadores de rate limit, adaptada de
 * {@code ArmazenamentoDeLimite} em {@code src/limite-compartilhado/armazenamento.ts} do Neves Shield.
 */
public interface RateLimitStorage {

    String name();

    boolean shared();

    RateLimitWindow increment(String key, long windowMs);

    void reset();
}
