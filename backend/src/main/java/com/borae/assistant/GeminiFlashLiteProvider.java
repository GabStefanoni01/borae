package com.borae.assistant;

import com.borae.assistant.AiProviderFailure.AiProviderException;
import com.borae.assistant.AiProviderFailure.FailureType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Adaptador stateless do Google Gemini Flash-Lite (Provedor Principal — Prioridade 1)
 * sobre o endpoint REST oficial {@code POST /v1beta/models/{model}:generateContent}.
 * Recebe apenas contexto público delimitado do catálogo. A chave de API é enviada
 * estritamente no cabeçalho {@code x-goog-api-key} e nunca em URLs, logs ou DTOs.
 */
@Component
public class GeminiFlashLiteProvider implements AiProvider {

    static final String DEFAULT_API_BASE_URL = "https://generativelanguage.googleapis.com";
    static final String DEFAULT_MODEL = "gemini-2.5-flash-lite";
    static final long DEFAULT_TIMEOUT_MS = 8_000L;

    private static final Set<String> SAFETY_FINISH_REASONS = Set.of(
            "SAFETY", "RECITATION", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII", "IMAGE_SAFETY"
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final boolean enabled;
    private final boolean dataProcessingApproved;
    private final int maxOutputTokens;

    @Autowired
    public GeminiFlashLiteProvider(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${app.assistant.gemini.api-key:}") String apiKey,
            @Value("${app.assistant.gemini.model:" + DEFAULT_MODEL + "}") String model,
            @Value("${app.assistant.gemini.enabled:false}") boolean enabled,
            @Value("${app.assistant.gemini.data-processing-approved:false}") boolean dataProcessingApproved,
            @Value("${app.assistant.gemini.timeout-ms:0}") long timeoutMs,
            @Value("${app.assistant.gemini.timeout-seconds:12}") long timeoutSeconds,
            @Value("${app.assistant.gemini.max-output-tokens:500}") int maxOutputTokens
    ) {
        this(configureTimeoutFactory(builder, resolveTimeoutMs(timeoutMs, timeoutSeconds)),
                DEFAULT_API_BASE_URL, objectMapper, apiKey, model, enabled, dataProcessingApproved, maxOutputTokens);
    }

    GeminiFlashLiteProvider(
            RestClient.Builder builder,
            String baseUrl,
            ObjectMapper objectMapper,
            String apiKey,
            String model,
            boolean enabled,
            boolean dataProcessingApproved,
            int maxOutputTokens
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = (model == null || model.isBlank()) ? DEFAULT_MODEL : model.trim();
        this.enabled = enabled;
        this.dataProcessingApproved = dataProcessingApproved;
        this.maxOutputTokens = Math.max(64, Math.min(maxOutputTokens, 1_000));
    }

    private static long resolveTimeoutMs(long timeoutMs, long timeoutSeconds) {
        if (timeoutMs > 0) {
            return Math.max(1_000L, timeoutMs);
        }
        return Math.max(1_000L, timeoutSeconds * 1_000L);
    }

    private static RestClient.Builder configureTimeoutFactory(RestClient.Builder builder, long timeoutMs) {
        Duration duration = Duration.ofMillis(Math.max(1_000L, timeoutMs));
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(duration)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(duration);
        return builder.requestFactory(requestFactory);
    }

    @Override
    public AiProviderId id() {
        return AiProviderId.GEMINI;
    }

    @Override
    public boolean isAvailable() {
        return enabled && dataProcessingApproved && apiKey != null && !apiKey.isBlank();
    }

    public String getModel() {
        return model;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    @Override
    public AiProviderResponse recommend(AiProviderRequest request) {
        if (!isAvailable()) {
            throw new AiProviderException(AiProviderId.GEMINI, FailureType.SERVICE_UNAVAILABLE,
                    "Provedor Gemini desabilitado ou não configurado.");
        }

        int effectiveMaxTokens = Math.max(64, Math.min(
                request.maxOutputTokens() > 0 ? request.maxOutputTokens() : maxOutputTokens, 1_000));

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("temperature", 0.3);
        generationConfig.put("maxOutputTokens", effectiveMaxTokens);
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", responseSchema());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("system_instruction", Map.of(
                "parts", List.of(Map.of("text", request.systemInstruction()))
        ));
        payload.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", request.buildUserPrompt()))
        )));
        payload.put("generationConfig", generationConfig);

        try {
            JsonNode response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", model)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            checkSafetyRefusal(response);
            String output = extractOutputText(response);
            if (output == null || output.isBlank()) {
                throw new AiProviderException(AiProviderId.GEMINI, FailureType.MALFORMED_RESPONSE,
                        "Resposta vazia do provedor Gemini.");
            }
            return parseNormalizedResponse(output, response);
        } catch (AiProviderException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw classifyHttpFailure(exception.getStatusCode().value());
        } catch (ResourceAccessException exception) {
            if (isTimeoutCause(exception)) {
                throw new AiProviderException(AiProviderId.GEMINI, FailureType.TIMEOUT,
                        "Tempo limite excedido ao consultar o provedor Gemini.");
            }
            throw new AiProviderException(AiProviderId.GEMINI, FailureType.SERVICE_UNAVAILABLE,
                    "Falha de rede ao consultar o provedor Gemini.");
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new AiProviderException(AiProviderId.GEMINI, FailureType.SERVICE_UNAVAILABLE,
                    "Erro técnico ao consultar o provedor Gemini.");
        }
    }

    /**
     * Sobrecarga de compatibilidade preservada para chamadores e testes existentes.
     */
    public ModelAnswer recommend(String userMessage, String catalogJson) {
        try {
            AiProviderResponse normalized = recommend(AiProviderRequest.forCatalog(userMessage, catalogJson, maxOutputTokens));
            List<ModelRecommendation> mapped = normalized.recommendations().stream()
                    .map(rec -> new ModelRecommendation(rec.productId(), rec.reason()))
                    .toList();
            return new ModelAnswer(normalized.answer(), mapped, normalized.inputTokens(), normalized.outputTokens());
        } catch (AiProviderException exception) {
            throw new AssistantProviderUnavailableException(exception);
        }
    }

    private void checkSafetyRefusal(JsonNode response) {
        if (response == null) return;
        String blockReason = response.path("promptFeedback").path("blockReason").asText("");
        if (!blockReason.isBlank() && !"BLOCK_REASON_UNSPECIFIED".equalsIgnoreCase(blockReason)) {
            throw new AiProviderException(AiProviderId.GEMINI, FailureType.SAFETY_REFUSAL,
                    "Solicitação recusada pela política de segurança do provedor Gemini.");
        }
        JsonNode candidates = response.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            String finishReason = candidates.get(0).path("finishReason").asText("");
            if (SAFETY_FINISH_REASONS.contains(finishReason.toUpperCase())) {
                throw new AiProviderException(AiProviderId.GEMINI, FailureType.SAFETY_REFUSAL,
                        "Resposta bloqueada pela política de segurança do provedor Gemini.");
            }
        }
    }

    private AiProviderException classifyHttpFailure(int status) {
        if (status == 429) {
            return new AiProviderException(AiProviderId.GEMINI, FailureType.RATE_LIMITED, status,
                    "Limite de requisições atingido no provedor Gemini.");
        }
        if (status == 401 || status == 403 || status == 400 || status == 404) {
            return new AiProviderException(AiProviderId.GEMINI, FailureType.AUTH_OR_CONFIG_ERROR, status,
                    "Erro de autenticação ou configuração no provedor Gemini.");
        }
        if (status >= 500) {
            return new AiProviderException(AiProviderId.GEMINI, FailureType.HTTP_5XX, status,
                    "Erro interno temporário no provedor Gemini.");
        }
        return new AiProviderException(AiProviderId.GEMINI, FailureType.SERVICE_UNAVAILABLE, status,
                "Indisponibilidade temporária no provedor Gemini.");
    }

    private static boolean isTimeoutCause(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor instanceof HttpTimeoutException || cursor instanceof SocketTimeoutException) {
                return true;
            }
            String message = cursor.getMessage();
            if (message != null && message.toLowerCase().contains("timed out")) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private Map<String, Object> responseSchema() {
        Map<String, Object> recommendation = new LinkedHashMap<>();
        recommendation.put("type", "OBJECT");
        recommendation.put("properties", Map.of(
                "productId", Map.of("type", "STRING"),
                "reason", Map.of("type", "STRING")
        ));
        recommendation.put("required", List.of("productId", "reason"));

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("type", "OBJECT");
        root.put("properties", Map.of(
                "answer", Map.of("type", "STRING"),
                "recommendations", Map.of(
                        "type", "ARRAY",
                        "items", recommendation
                )
        ));
        root.put("required", List.of("answer", "recommendations"));
        return root;
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) return null;

        JsonNode candidates = response.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (parts.isArray()) {
                StringBuilder combined = new StringBuilder();
                for (JsonNode part : parts) {
                    String text = part.path("text").asText(null);
                    if (text != null && !text.isBlank()) {
                        combined.append(text);
                    }
                }
                if (!combined.isEmpty()) {
                    return combined.toString().trim();
                }
            }
        }

        String direct = response.path("output_text").asText(null);
        if (direct != null && !direct.isBlank()) {
            return direct.trim();
        }
        return null;
    }

    private AiProviderResponse parseNormalizedResponse(String rawOutput, JsonNode response) {
        try {
            String cleaned = stripCodeFences(rawOutput);
            JsonNode root = objectMapper.readTree(cleaned);
            if (!root.isObject() || !root.has("answer") || !root.path("recommendations").isArray()) {
                throw new AiProviderException(AiProviderId.GEMINI, FailureType.MALFORMED_RESPONSE,
                        "Resposta do Gemini fora do contrato JSON esperado.");
            }
            String answer = root.path("answer").asText("");
            List<AiProviderResponse.RecommendationCandidate> recommendations = new ArrayList<>();
            for (JsonNode item : root.path("recommendations")) {
                if (recommendations.size() == 4) break;
                try {
                    UUID productId = UUID.fromString(item.path("productId").asText("").trim());
                    String reason = item.path("reason").asText("");
                    recommendations.add(new AiProviderResponse.RecommendationCandidate(productId, reason));
                } catch (IllegalArgumentException ignored) {
                    // IDs inválidos são descartados; o serviço valida pertencimento ao catálogo.
                }
            }

            JsonNode usageMetadata = response == null ? null : response.path("usageMetadata");
            long inputTokens = 0;
            long outputTokens = 0;
            if (usageMetadata != null && !usageMetadata.isMissingNode()) {
                inputTokens = Math.max(0, usageMetadata.path("promptTokenCount").asLong(0));
                outputTokens = Math.max(0, usageMetadata.path("candidatesTokenCount").asLong(0));
            } else if (response != null && !response.path("usage").isMissingNode()) {
                JsonNode usage = response.path("usage");
                inputTokens = Math.max(0, usage.path("total_input_tokens").asLong(0));
                outputTokens = Math.max(0, usage.path("total_output_tokens").asLong(0));
            }
            return AiProviderResponse.of(AiProviderId.GEMINI, answer, recommendations, inputTokens, outputTokens);
        } catch (AiProviderException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiProviderException(AiProviderId.GEMINI, FailureType.MALFORMED_RESPONSE,
                    "JSON malformado retornado pelo provedor Gemini.");
        }
    }

    static String stripCodeFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    public record ModelAnswer(String answer, List<ModelRecommendation> recommendations,
                              long inputTokens, long outputTokens) {
        public ModelAnswer(String answer, List<ModelRecommendation> recommendations) {
            this(answer, recommendations, 0, 0);
        }
    }

    public record ModelRecommendation(UUID productId, String reason) {}

    public static class AssistantProviderUnavailableException extends RuntimeException {
        public AssistantProviderUnavailableException() {
            super("O serviço de recomendação está temporariamente indisponível.");
        }

        public AssistantProviderUnavailableException(Throwable cause) {
            super("O serviço de recomendação está temporariamente indisponível.", cause);
        }
    }
}
