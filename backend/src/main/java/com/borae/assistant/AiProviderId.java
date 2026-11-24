package com.borae.assistant;

import java.util.Locale;

/**
 * Identificadores oficiais dos provedores de IA suportados pelo Assistente Gastronômico.
 */
public enum AiProviderId {
    GEMINI("gemini"),
    GROK("grok");

    private final String key;

    AiProviderId(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static AiProviderId fromKey(String raw, AiProviderId defaultValue) {
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        for (AiProviderId id : values()) {
            if (id.key.equals(normalized)) {
                return id;
            }
        }
        return defaultValue;
    }
}
