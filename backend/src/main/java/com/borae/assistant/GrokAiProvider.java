package com.borae.assistant;

import com.borae.assistant.AiProviderFailure.AiProviderException;
import com.borae.assistant.AiProviderFailure.FailureType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Adaptador stateless do Grok / xAI (Provedor Secundário de Contingência — Prioridade 2)
 * sobre a interface recomendada oficialmente pela xAI: <strong>Responses API</strong>
 * ({@code POST {baseUrl}/v1/responses}).
 *
 * <p>Envia explicitamente {@code store=false} para preservar a política D-11 (sem persistência
 * de conversas no provedor), utiliza structured outputs via {@code text.format} ({@code json_schema}),
 * respeita o {@link AiRequestDeadline} global com cancelamento HTTP real e permanece desabilitado
 * por padrão até homologação futura.</p>
 */
@Component
public class GrokAiProvider implements AiProvider {

    static final String DEFAULT_API_BASE_URL = "https://api.x.ai";
    static final String DEFAULT_MODEL = "grok-4.3";
    static final long DEFAULT_TIMEOUT_MS = 8_000L;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final boolean enabled;
    private final boolean dataProcessingApproved;
    private final long timeoutMs;
    private final int maxOutputTokens;

    @Autowired
    public GrokAiProvider(
            RestClient.Builder builder,
            ObjectMapper objectMapper,
            @Value("${app.assistant.grok.base-url:" + DEFAULT_API_BASE_URL + "}") String baseUrl,
            @Value("${app.assistant.grok.api-key:}") String apiKey,
            @Value("${app.assistant.grok.model:" + DEFAULT_MODEL + "}") String model,
            @Value("${app.assistant.grok.enabled:false}") boolean enabled,
            @Value("${app.assistant.grok.data-processing-approved:false}") boolean dataProcessingApproved,
            @Value("${app.assistant.grok.timeout-ms:" + DEFAULT_TIMEOUT_MS + "}") long timeoutMs,
            @Value("${app.assistant.grok.max-output-tokens:500}") int maxOutputTokens
    ) {
        this(configureDeadlineFactory(builder, timeoutMs), normalizeBaseUrl(baseUrl), objectMapper,
                apiKey, model, enabled, dataProcessingApproved, timeoutMs, maxOutputTokens);
    }

    GrokAiProvider(
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

    GrokAiProvider(
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
        this.restClient = builder.baseUrl(normalizeBaseUrl(baseUrl)).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = (model == null || model.isBlank()) ? DEFAULT_MODEL : model.trim();
        this.enabled = enabled;
        this.dataProcessingApproved = dataProcessingApproved;
        this.timeoutMs = Math.max(1L, timeoutMs);
        this.maxOutputTokens = Math.max(64, Math.min(maxOutputTokens, 1_000));
    }

    static GrokAiProvider withDeadlineAwareHttpClient(
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
        return new GrokAiProvider(builder, baseUrl, objectMapper, apiKey, model,
                enabled, dataProcessingApproved, timeoutMs, maxOutputTokens);
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return DEFAULT_API_BASE_URL;
        }
        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static RestClient.Builder configureDeadlineFactory(RestClient.Builder builder, long configuredTimeoutMs) {
        long safeTimeout = Math.max(100L, configuredTimeoutMs);
        return builder.requestFactory(DeadlineAwareClientHttpRequestFactory.create(safeTimeout, safeTimeout));
    }

    @Override
    public AiProviderId id() {
        return AiProviderId.GROK;
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
            throw new AiProviderException(AiProviderId.GROK, FailureType.SERVICE_UNAVAILABLE,
                    "Provedor Grok desabilitado ou não configurado.");
        }

        long effectiveTimeoutMs = resolveEffectiveTimeoutMs(request.deadline());
        if (effectiveTimeoutMs <= 0L) {
            throw new AiProviderException(AiProviderId.GROK, FailureType.TIMEOUT,
                    "Orçamento global de tempo expirado antes de consultar o provedor Grok.");
        }

        int effectiveMaxTokens = Math.max(64, Math.min(
                request.maxOutputTokens() > 0 ? request.maxOutputTokens() : maxOutputTokens, 1_000));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("store", false); // D-11: nunca armazenar conversas no servidor do provedor
        payload.put("temperature", 0.3);
        payload.put("max_output_tokens", effectiveMaxTokens);
        payload.put("instructions", request.systemInstruction());
        payload.put("input", request.buildUserPrompt());
        payload.put("text", Map.of("format", structuredOutputFormat()));

        try {
            JsonNode response = DeadlineAwareClientHttpRequestFactory.executeWithTimeout(effectiveTimeoutMs, () ->
                    restClient.post()
                            .uri("/v1/responses")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(payload)
                            .retrieve()
                            .body(JsonNode.class)
            );

            long[] tokens = extractTokenUsage(response);
            checkSafetyRefusal(response, tokens[0], tokens[1]);
            String output = extractOutputText(response);
            if (output == null || output.isBlank()) {
                throw new AiProviderException(AiProviderId.GROK, FailureType.MALFORMED_RESPONSE, null,
                        "Resposta vazia do provedor Grok.", tokens[0], tokens[1], List.of());
            }
            return parseNormalizedResponse(output, tokens[0], tokens[1]);
        } catch (AiProviderException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw classifyHttpFailure(exception.getStatusCode().value());
        } catch (ResourceAccessException exception) {
            if (GeminiFlashLiteProvider.isTimeoutCause(exception)) {
                throw new AiProviderException(AiProviderId.GROK, FailureType.TIMEOUT,
                        "Tempo limite excedido ao consultar o provedor Grok.");
            }
            throw new AiProviderException(AiProviderId.GROK, FailureType.SERVICE_UNAVAILABLE,
                    "Falha de rede ao consultar o provedor Grok.");
        } catch (RestClientException | IllegalArgumentException exception) {
            if (GeminiFlashLiteProvider.isTimeoutCause(exception)) {
                throw new AiProviderException(AiProviderId.GROK, FailureType.TIMEOUT,
                        "Tempo limite excedido ao consultar o provedor Grok.");
            }
            throw new AiProviderException(AiProviderId.GROK, FailureType.SERVICE_UNAVAILABLE,
                    "Erro técnico ao consultar o provedor Grok.");
        }
    }

    long resolveEffectiveTimeoutMs(AiRequestDeadline deadline) {
        return deadline == null ? timeoutMs : deadline.timeoutForProvider(timeoutMs);
    }

    private void checkSafetyRefusal(JsonNode response, long inputTokens, long outputTokens) {
        if (response == null) return;

        String incompleteReason = response.path("incomplete_details").path("reason").asText("");
        if ("content_filter".equalsIgnoreCase(incompleteReason)) {
            throw new AiProviderException(AiProviderId.GROK, FailureType.SAFETY_REFUSAL, null,
                    "Solicitação recusada pela política de segurança do provedor Grok.",
                    inputTokens, outputTokens, List.of());
        }

        JsonNode outputs = response.path("output");
        if (outputs.isArray()) {
            for (JsonNode item : outputs) {
                JsonNode content = item.path("content");
                if (content.isArray()) {
                    for (JsonNode part : content) {
                        String type = part.path("type").asText("");
                        String refusal = part.path("refusal").asText("");
                        if ("refusal".equalsIgnoreCase(type) || !refusal.isBlank()) {
                            throw new AiProviderException(AiProviderId.GROK, FailureType.SAFETY_REFUSAL, null,
                                    "Solicitação recusada pela política de segurança do provedor Grok.",
                                    inputTokens, outputTokens, List.of());
                        }
                    }
                }
            }
        }
    }

    private AiProviderException classifyHttpFailure(int status) {
        if (status == 429) {
            return new AiProviderException(AiProviderId.GROK, FailureType.RATE_LIMITED, status,
                    "Limite de requisições atingido no provedor Grok.");
        }
        if (status == 401 || status == 403 || status == 400 || status == 404) {
            return new AiProviderException(AiProviderId.GROK, FailureType.AUTH_OR_CONFIG_ERROR, status,
                    "Erro de autenticação ou configuração no provedor Grok.");
        }
        if (status >= 500) {
            return new AiProviderException(AiProviderId.GROK, FailureType.HTTP_5XX, status,
                    "Erro interno temporário no provedor Grok.");
        }
        return new AiProviderException(AiProviderId.GROK, FailureType.SERVICE_UNAVAILABLE, status,
                "Indisponibilidade temporária no provedor Grok.");
    }

    private Map<String, Object> structuredOutputFormat() {
        Map<String, Object> recommendation = new LinkedHashMap<>();
        recommendation.put("type", "object");
        recommendation.put("properties", Map.of(
                "productId", Map.of("type", "string"),
                "reason", Map.of("type", "string")
        ));
        recommendation.put("required", List.of("productId", "reason"));
        recommendation.put("additionalProperties", false);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Map.of(
                "answer", Map.of("type", "string"),
                "recommendations", Map.of(
                        "type", "array",
                        "items", recommendation
                )
        ));
        schema.put("required", List.of("answer", "recommendations"));
        schema.put("additionalProperties", false);

        Map<String, Object> format = new LinkedHashMap<>();
        format.put("type", "json_schema");
        format.put("name", "gastronomic_recommendation");
        format.put("strict", true);
        format.put("schema", schema);
        return format;
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) return null;

        // Formato nativo Responses API: output[].content[].text (type="output_text")
        JsonNode outputs = response.path("output");
        if (outputs.isArray()) {
            StringBuilder combined = new StringBuilder();
            for (JsonNode item : outputs) {
                JsonNode content = item.path("content");
                if (content.isArray()) {
                    for (JsonNode part : content) {
                        String type = part.path("type").asText("");
                        if ("output_text".equalsIgnoreCase(type) || type.isBlank()) {
                            String text = part.path("text").asText(null);
                            if (text != null && !text.isBlank()) {
                                combined.append(text);
                            }
                        }
                    }
                }
            }
            if (!combined.isEmpty()) {
                return combined.toString().trim();
            }
        }

        // Fallback para campo agregado output_text quando presente
        String direct = response.path("output_text").asText(null);
        if (direct != null && !direct.isBlank()) {
            return direct.trim();
        }
        return null;
    }

    private long[] extractTokenUsage(JsonNode response) {
        if (response == null) return new long[]{0L, 0L};
        JsonNode usage = response.path("usage");
        if (usage.isMissingNode()) return new long[]{0L, 0L};
        long inputTokens = Math.max(0L, usage.path("input_tokens").asLong(
                usage.path("prompt_tokens").asLong(0L)
        ));
        long outputTokens = Math.max(0L, usage.path("output_tokens").asLong(
                usage.path("completion_tokens").asLong(0L)
        ));
        return new long[]{inputTokens, outputTokens};
    }

    private AiProviderResponse parseNormalizedResponse(String rawOutput, long inputTokens, long outputTokens) {
        try {
            String cleaned = GeminiFlashLiteProvider.stripCodeFences(rawOutput);
            JsonNode root = objectMapper.readTree(cleaned);
            if (!root.isObject() || !root.has("answer") || !root.path("recommendations").isArray()) {
                throw new AiProviderException(AiProviderId.GROK, FailureType.MALFORMED_RESPONSE, null,
                        "Resposta do Grok fora do contrato JSON esperado.", inputTokens, outputTokens, List.of());
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

            return AiProviderResponse.of(AiProviderId.GROK, answer, recommendations, inputTokens, outputTokens);
        } catch (AiProviderException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiProviderException(AiProviderId.GROK, FailureType.MALFORMED_RESPONSE, null,
                    "JSON malformado retornado pelo provedor Grok.", inputTokens, outputTokens, List.of());
        }
    }
}
