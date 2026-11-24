package com.borae.assistant;

import com.borae.assistant.AiProviderFailure.AiProviderException;
import com.borae.assistant.AiProviderFailure.FailureType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Adaptador stateless do Google Gemini Flash-Lite (Provedor Principal — Prioridade 1)
 * sobre o endpoint REST oficial {@code POST /v1beta/models/{model}:generateContent}.
 *
 * <p>Respeita o {@link AiRequestDeadline} monotônico global limitando cada requisição HTTP ao menor valor
 * entre seu timeout individual e o orçamento restante, com cancelamento real do stream HTTP
 * via {@link DeadlineAwareClientHttpRequestFactory}.</p>
 */
@Component
public class GeminiFlashLiteProvider implements AiProvider {

    static final String DEFAULT_API_BASE_URL = "https://generativelanguage.googleapis.com";
    static final String DEFAULT_MODEL = "gemini-2.5-flash-lite";
    static final long DEFAULT_TIMEOUT_MS = 12_000L;

    private static final Set<String> SAFETY_FINISH_REASONS = Set.of(
            "SAFETY", "RECITATION", "PROHIBITED_CONTENT", "BLOCKLIST", "SPII", "IMAGE_SAFETY"
    );

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final boolean enabled;
    private final boolean dataProcessingApproved;
    private final long timeoutMs;
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
        this(configureDeadlineFactory(builder, resolveTimeoutMs(timeoutMs, timeoutSeconds)),
                DEFAULT_API_BASE_URL, objectMapper, apiKey, model, enabled, dataProcessingApproved,
                resolveTimeoutMs(timeoutMs, timeoutSeconds), maxOutputTokens);
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
        this(builder, baseUrl, objectMapper, apiKey, model, enabled, dataProcessingApproved,
                DEFAULT_TIMEOUT_MS, maxOutputTokens);
    }

    GeminiFlashLiteProvider(
            RestClient.Builder builder,
            String baseUrl,
            ObjectMapper objectMapper,
            String apiKey,
            String model,
            boolean enabled,
            boolean dataProcessingApproved,
            long timeoutMs,
            int maxOutputTokens
    ) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = (model == null || model.isBlank()) ? DEFAULT_MODEL : model.trim();
        this.enabled = enabled;
        this.dataProcessingApproved = dataProcessingApproved;
        this.timeoutMs = Math.max(1L, timeoutMs);
        this.maxOutputTokens = Math.max(64, Math.min(maxOutputTokens, 1_000));
    }

    static GeminiFlashLiteProvider withDeadlineAwareHttpClient(
            String baseUrl,
            ObjectMapper objectMapper,
            String apiKey,
            String model,
            boolean enabled,
            boolean dataProcessingApproved,
            long timeoutMs,
            int maxOutputTokens
    ) {
        RestClient.Builder builder = configureDeadlineFactory(RestClient.builder(), timeoutMs);
        return new GeminiFlashLiteProvider(builder, baseUrl, objectMapper, apiKey, model,
                enabled, dataProcessingApproved, timeoutMs, maxOutputTokens);
    }

    private static long resolveTimeoutMs(long timeoutMs, long timeoutSeconds) {
        if (timeoutMs > 0) {
            return Math.max(100L, timeoutMs);
        }
        return Math.max(100L, timeoutSeconds * 1_000L);
    }

    private static RestClient.Builder configureDeadlineFactory(RestClient.Builder builder, long configuredTimeoutMs) {
        long safeTimeout = Math.max(100L, configuredTimeoutMs);
        return builder.requestFactory(DeadlineAwareClientHttpRequestFactory.create(safeTimeout, safeTimeout));
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

    public long getTimeoutMs() {
        return timeoutMs;
    }

    public int getMaxOutputTokens() {
        return maxOutputTokens;
    }

    @Override
    public AiProviderResponse recommend(AiProviderRequest request) {
        if (!isAvailable()) {
            throw AiProviderException.skipped(AiProviderId.GEMINI, FailureType.SERVICE_UNAVAILABLE,
                    "Provedor Gemini desabilitado ou não configurado.");
        }

        long effectiveTimeoutMs = resolveEffectiveTimeoutMs(request.deadline());
        if (effectiveTimeoutMs <= 0L) {
            throw AiProviderException.skipped(AiProviderId.GEMINI, FailureType.TIMEOUT,
                    "Orçamento global de tempo expirado antes de consultar o provedor Gemini.");
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
            JsonNode response = DeadlineAwareClientHttpRequestFactory.executeWithTimeout(effectiveTimeoutMs, () ->
                    restClient.post()
                            .uri("/v1beta/models/{model}:generateContent", model)
                            .header("x-goog-api-key", apiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(payload)
                            .retrieve()
                            .body(JsonNode.class)
            );

            UsageInfo usage = extractTokenUsage(response);
            checkSafetyRefusal(response, usage);
            String output = extractOutputText(response);
            if (output == null || output.isBlank()) {
                throw new AiProviderException(AiProviderId.GEMINI, FailureType.MALFORMED_RESPONSE, null,
                        "Resposta vazia do provedor Gemini.",
                        usage.known(), usage.inputTokens(), usage.outputTokens(), 0L, List.of());
            }
            return parseNormalizedResponse(output, usage);
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
            if (isTimeoutCause(exception)) {
                throw new AiProviderException(AiProviderId.GEMINI, FailureType.TIMEOUT,
                        "Tempo limite excedido ao consultar o provedor Gemini.");
            }
            throw new AiProviderException(AiProviderId.GEMINI, FailureType.SERVICE_UNAVAILABLE,
                    "Erro técnico ao consultar o provedor Gemini.");
        }
    }

    long resolveEffectiveTimeoutMs(AiRequestDeadline deadline) {
        return deadline == null ? timeoutMs : deadline.timeoutForProvider(timeoutMs);
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

    private void checkSafetyRefusal(JsonNode response, UsageInfo usage) {
        if (response == null) return;
        String blockReason = response.path("promptFeedback").path("blockReason").asText("");
        if (!blockReason.isBlank() && !"BLOCK_REASON_UNSPECIFIED".equalsIgnoreCase(blockReason)) {
            throw new AiProviderException(AiProviderId.GEMINI, FailureType.SAFETY_REFUSAL, null,
                    "Solicitação recusada pela política de segurança do provedor Gemini.",
                    usage.known(), usage.inputTokens(), usage.outputTokens(), 0L, List.of());
        }
        JsonNode candidates = response.path("candidates");
        if (candidates.isArray() && !candidates.isEmpty()) {
            String finishReason = candidates.get(0).path("finishReason").asText("");
            if (SAFETY_FINISH_REASONS.contains(finishReason.toUpperCase())) {
                throw new AiProviderException(AiProviderId.GEMINI, FailureType.SAFETY_REFUSAL, null,
                        "Resposta bloqueada pela política de segurança do provedor Gemini.",
                        usage.known(), usage.inputTokens(), usage.outputTokens(), 0L, List.of());
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

    static boolean isTimeoutCause(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null) {
            if (cursor instanceof HttpTimeoutException
                    || cursor instanceof SocketTimeoutException
                    || cursor instanceof java.util.concurrent.TimeoutException) {
                return true;
            }
            String message = cursor.getMessage();
            if (message != null) {
                String lower = message.toLowerCase();
                if (lower.contains("timed out") || lower.contains("timeout")) {
                    return true;
                }
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

    private UsageInfo extractTokenUsage(JsonNode response) {
        if (response == null) return UsageInfo.UNKNOWN;

        JsonNode usageMetadata = response.path("usageMetadata");
        if (usageMetadata.isObject() && (usageMetadata.has("promptTokenCount") || usageMetadata.has("candidatesTokenCount"))) {
            long input = Math.max(0L, usageMetadata.path("promptTokenCount").asLong(0L));
            long output = Math.max(0L, usageMetadata.path("candidatesTokenCount").asLong(0L));
            return new UsageInfo(true, input, output);
        }
        JsonNode usage = response.path("usage");
        if (usage.isObject() && (usage.has("total_input_tokens") || usage.has("total_output_tokens"))) {
            long input = Math.max(0L, usage.path("total_input_tokens").asLong(0L));
            long output = Math.max(0L, usage.path("total_output_tokens").asLong(0L));
            return new UsageInfo(true, input, output);
        }
        return UsageInfo.UNKNOWN;
    }

    private AiProviderResponse parseNormalizedResponse(String rawOutput, UsageInfo usage) {
        try {
            String cleaned = stripCodeFences(rawOutput);
            JsonNode root = objectMapper.readTree(cleaned);
            if (!root.isObject() || !root.has("answer") || !root.path("recommendations").isArray()) {
                throw new AiProviderException(AiProviderId.GEMINI, FailureType.MALFORMED_RESPONSE, null,
                        "Resposta do Gemini fora do contrato JSON esperado.",
                        usage.known(), usage.inputTokens(), usage.outputTokens(), 0L, List.of());
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
            return AiProviderResponse.of(
                    AiProviderId.GEMINI, answer, recommendations,
                    usage.known(), usage.inputTokens(), usage.outputTokens(), 0L
            );
        } catch (AiProviderException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiProviderException(AiProviderId.GEMINI, FailureType.MALFORMED_RESPONSE, null,
                    "JSON malformado retornado pelo provedor Gemini.",
                    usage.known(), usage.inputTokens(), usage.outputTokens(), 0L, List.of());
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

    private record UsageInfo(boolean known, long inputTokens, long outputTokens) {
        static final UsageInfo UNKNOWN = new UsageInfo(false, 0L, 0L);
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
