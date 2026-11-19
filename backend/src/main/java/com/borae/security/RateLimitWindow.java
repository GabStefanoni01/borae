package com.borae.security;

/**
 * Estado de uma janela de contagem de rate limit após incremento,
 * adaptado de {@code JanelaDeLimite} em {@code src/limite-compartilhado/armazenamento.ts} do Neves Shield.
 */
public record RateLimitWindow(int count, long resetAtEpochMs) {
}
