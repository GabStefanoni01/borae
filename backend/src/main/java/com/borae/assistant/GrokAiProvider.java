package com.borae.assistant;

import com.borae.assistant.AiProviderFailure.AiProviderException;
import com.borae.assistant.AiProviderFailure.FailureType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
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
import java.util.UUID;

/**
 * Adaptador stateless do Grok / xAI (Provedor Secundário de Contingência — Prioridade 2)
 * sobre o endpoint REST compatível {@code POST {baseUrl}/v1/chat/completions}.
 *
 * <p>Permanece desabilitado por padrão até validação futura dos termos, modelo oficial e
 * credenciais em staging/produção. Nunca é acionado antes do Gemini. A chave de API é
 * enviada apenas no cabeçalho {@code Authorization: Bearer} e nunca em URLs, logs ou DTOs.</p>
 */
@Component
public class GrokAiProvider implements AiProvider {

    static final String DEFAULT_API_BASE_URL = "https://api.x.ai";
    static final String DEFAULT_MODEL = "grok-3-mini";
    static final long DEFAULT_TIMEOUT_MS = 8_000L;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final boolean enabled;
    private final boolean dataProcessingApproved;
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
        this(configureTimeoutFactory(builder, timeoutMs), normalizeBaseUrl(baseUrl), objectMapper,
                apiKey, model, enabled, dataProcessingApproved, maxOutputTokens);
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
        this.restClient = builder.baseUrl(normalizeBaseUrl(baseUrl)).build();
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = (model == null || model.isBlank()) ? DEFAULT_MODEL : model.trim();
        this.enabled = enabled;
        this.dataProcessingApproved = dataProcessingApproved;
        this.maxOutputTokens = Math.max(64, Math.min(maxOutputTokens, 1_000));
    }

    private static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return DEFAULT_API_BASE_URL;
        }
        String trimmed = baseUrl.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
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
        return AiProviderId.GROK;
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
            throw new AiProviderException(AiProviderId.GROK, FailureType.SERVICE_UNAVAILABLE,
                    "Provedor Grok desabilitado ou não configurado.");
        }

        int effectiveMaxTokens = Math.max(64, Math.min(
                request.maxOutputTokens() > 0 ? request.maxOutputTokens() : maxOutputTokens, 1_000));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", model);
        payload.put("temperature", 0.3);
        payload.put("max_tokens", effectiveMaxTokens);
        payload.put("messages", List.of(
                Map.of("role", "system", "content", request.systemInstruction()),
                Map.of("role", "user", "content", request.buildUserPrompt())
        ));
        payload.put("response_format", responseFormatSchema());

        try {
            JsonNode response = restClient.post()
                    .uri("/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);

            checkSafetyRefusal(response);
            String output = extractOutputText(response);
            if (output == null || output.isBlank()) {
                throw new AiProviderException(AiProviderId.GROK, FailureType.MALFORMED_RESPONSE,
                        "Resposta vazia do provedor Grok.");
            }
            return parseNormalizedResponse(output, response);
        } catch (AiProviderException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw classifyHttpFailure(exception.getStatusCode().value());
        } catch (ResourceAccessException exception) {
            if (isTimeoutCause(exception)) {
                throw new AiProviderException(AiProviderId.GROK, FailureType.TIMEOUT,
                        "Tempo limite excedido ao consultar o provedor Grok.");
            }
            throw new AiProviderException(AiProviderId.GROK, FailureType.SERVICE_UNAVAILABLE,
                    "Falha de rede ao consultar o provedor Grok.");
        } catch (RestClientException | IllegalArgumentException exception) {
            throw new AiProviderException(AiProviderId.GROK, FailureType.SERVICE_UNAVAILABLE,
                    "Erro técnico ao consultar o provedor Grok.");
        }
    }

    private void checkSafetyRefusal(JsonNode response) {
        if (response == null) return;
        JsonNode choices = response.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode first = choices.get(0);
            String finishReason = first.path("finish_reason").asText("");
            String refusal = first.path("message").path("refusal").asText("");
            if ("content_filter".equalsIgnoreCase(finishReason) || !refusal.isBlank()) {
                throw new AiProviderException(AiProviderId.GROK, FailureType.SAFETY_REFUSAL,
                        "Solicitação recusada pela política de segurança do provedor Grok.");
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

    private Map<String, Object> responseFormatSchema() {
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

        return Map.of(
                "type", "json_schema",
                "json_schema", Map.of(
                        "name", "gastronomic_recommendation",
                        "strict", true,
                        "schema", schema
                )
        );
    }

    private String extractOutputText(JsonNode response) {
        if (response == null) return null;
        JsonNode choices = response.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            String content = choices.get(0).path("message").path("content").asText(null);
            if (content != null && !content.isBlank()) {
                return content.trim();
            }
        }
        return null;
    }

    private AiProviderResponse parseNormalizedResponse(String rawOutput, JsonNode response) {
        try {
            String cleaned = GeminiFlashLiteProvider.stripCodeFences(rawOutput);
            JsonNode root = objectMapper.readTree(cleaned);
            if (!root.isObject() || !root.has("answer") || !root.path("recommendations").isArray()) {
                throw new AiProviderException(AiProviderId.GROK, FailureType.MALFORMED_RESPONSE,
                        "Resposta do Grok fora do contrato JSON esperado.");
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

            JsonNode usage = response == null ? null : response.path("usage");
            long inputTokens = usage == null || usage.isMissingNode()
                    ? 0L : Math.max(0L, usage.path("prompt_tokens").asLong(0L));
            long outputTokens = usage == null || usage.isMissingNode()
                    ? 0L : Math.max(0L, usage.path("completion_tokens").asLong(0L));

            return AiProviderResponse.of(AiProviderId.GROK, answer, recommendations, inputTokens, outputTokens);
        } catch (AiProviderException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AiProviderException(AiProviderId.GROK, FailureType.MALFORMED_RESPONSE,
                    "JSON malformado retornado pelo provedor Grok.");
        }
    }
}
