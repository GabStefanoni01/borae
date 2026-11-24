package com.borae.assistant;

/**
 * Interface comum para adaptadores de provedores de IA do Assistente Gastronômico.
 * Mantém o domínio isolado de detalhes de protocolo, SDKs e formatos nativos de cada fornecedor.
 */
public interface AiProvider {

    AiProviderId id();

    boolean isAvailable();

    AiProviderResponse recommend(AiProviderRequest request);
}
