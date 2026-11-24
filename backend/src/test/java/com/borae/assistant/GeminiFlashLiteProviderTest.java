package com.borae.assistant;

import com.borae.assistant.AiProviderFailure.AiProviderException;
import com.borae.assistant.AiProviderFailure.FailureType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class GeminiFlashLiteProviderTest {

    private static final String TEST_API_KEY = "test-only-synthetic-gemini-key";
    private static final UUID PRODUCT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void reportsUnavailableWhenDisabledOrMissingApprovalOrKey() {
        RestClient.Builder builder = RestClient.builder();
        assertFalse(new GeminiFlashLiteProvider(builder, "https://example.invalid", objectMapper,
                TEST_API_KEY, "gemini-2.5-flash-lite", false, true, 500).isAvailable());
        assertFalse(new GeminiFlashLiteProvider(builder, "https://example.invalid", objectMapper,
                TEST_API_KEY, "gemini-2.5-flash-lite", true, false, 500).isAvailable());
        assertFalse(new GeminiFlashLiteProvider(builder, "https://example.invalid", objectMapper,
                "", "gemini-2.5-flash-lite", true, true, 500).isAvailable());
    }

    @Test
    void callsGenerateContentWithHeaderKeyAndMaxOutputTokensAndParsesStructuredResponse() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        GeminiFlashLiteProvider provider = new GeminiFlashLiteProvider(
                builder,
                "https://generativelanguage.googleapis.com",
                objectMapper,
                TEST_API_KEY,
                "gemini-2.5-flash-lite",
                true,
                true,
                500
        );

        String modelJson = """
                {"answer":"Recomendo o risoto leve de limão.","recommendations":[{"productId":"11111111-1111-1111-1111-111111111111","reason":"Prato leve com limão siciliano."}]}
                """;
        String geminiPayload = objectMapper.writeValueAsString(Map.of(
                "candidates", List.of(Map.of(
                        "content", Map.of(
                                "parts", List.of(Map.of("text", modelJson))
                        )
                )),
                "usageMetadata", Map.of(
                        "promptTokenCount", 312,
                        "candidatesTokenCount", 74
                )
        ));

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent"))
                .andExpect(requestTo(not(containsString(TEST_API_KEY))))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", TEST_API_KEY))
                .andExpect(jsonPath("$.generationConfig.maxOutputTokens").value(500))
                .andExpect(jsonPath("$.generationConfig.responseMimeType").value("application/json"))
                .andExpect(jsonPath("$.system_instruction.parts[0].text").exists())
                .andExpect(jsonPath("$.contents[0].parts[0].text").exists())
                .andRespond(withSuccess(geminiPayload, MediaType.APPLICATION_JSON));

        GeminiFlashLiteProvider.ModelAnswer answer = provider.recommend("Quero algo leve", "{\"products\":[]}");

        server.verify();
        assertEquals("Recomendo o risoto leve de limão.", answer.answer());
        assertEquals(1, answer.recommendations().size());
        assertEquals(PRODUCT_ID, answer.recommendations().getFirst().productId());
        assertEquals("Prato leve com limão siciliano.", answer.recommendations().getFirst().reason());
        assertEquals(312, answer.inputTokens());
        assertEquals(74, answer.outputTokens());
    }

    @Test
    void stripsMarkdownCodeFencesWhenModelWrapsJson() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        GeminiFlashLiteProvider provider = new GeminiFlashLiteProvider(
                builder, "https://generativelanguage.googleapis.com", objectMapper,
                TEST_API_KEY, "gemini-2.5-flash-lite", true, true, 500
        );

        String fenced = "```json\n{\"answer\":\"Boa escolha!\",\"recommendations\":[]}\n```";
        String geminiPayload = objectMapper.writeValueAsString(Map.of(
                "candidates", List.of(Map.of(
                        "content", Map.of(
                                "parts", List.of(Map.of("text", fenced))
                        )
                ))
        ));

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent"))
                .andRespond(withSuccess(geminiPayload, MediaType.APPLICATION_JSON));

        GeminiFlashLiteProvider.ModelAnswer answer = provider.recommend("Sugestão?", "{}");
        assertEquals("Boa escolha!", answer.answer());
        assertTrue(answer.recommendations().isEmpty());
    }

    @Test
    void classifiesSafetyRefusalRateLimitHttp5xxAndAuthErrorsCleanly() throws Exception {
        RestClient.Builder builderSafety = RestClient.builder();
        MockRestServiceServer serverSafety = MockRestServiceServer.bindTo(builderSafety).build();
        GeminiFlashLiteProvider providerSafety = new GeminiFlashLiteProvider(
                builderSafety, "https://generativelanguage.googleapis.com", objectMapper,
                TEST_API_KEY, "gemini-2.5-flash-lite", true, true, 500
        );
        String safetyPayload = objectMapper.writeValueAsString(Map.of(
                "candidates", List.of(Map.of("finishReason", "SAFETY"))
        ));
        serverSafety.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent"))
                .andRespond(withSuccess(safetyPayload, MediaType.APPLICATION_JSON));
        AiProviderException safetyEx = assertThrows(AiProviderException.class,
                () -> providerSafety.recommend(AiProviderRequest.forCatalog("pedido proibido", "{}", 500)));
        assertEquals(FailureType.SAFETY_REFUSAL, safetyEx.getFailure().type());
        assertFalse(safetyEx.getMessage().contains(TEST_API_KEY));

        RestClient.Builder builder429 = RestClient.builder();
        MockRestServiceServer server429 = MockRestServiceServer.bindTo(builder429).build();
        GeminiFlashLiteProvider provider429 = new GeminiFlashLiteProvider(
                builder429, "https://generativelanguage.googleapis.com", objectMapper,
                TEST_API_KEY, "gemini-2.5-flash-lite", true, true, 500
        );
        server429.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        AiProviderException err429 = assertThrows(AiProviderException.class,
                () -> provider429.recommend(AiProviderRequest.forCatalog("Quero massa", "{}", 500)));
        assertEquals(FailureType.RATE_LIMITED, err429.getFailure().type());

        RestClient.Builder builder5xx = RestClient.builder();
        MockRestServiceServer server5xx = MockRestServiceServer.bindTo(builder5xx).build();
        GeminiFlashLiteProvider provider5xx = new GeminiFlashLiteProvider(
                builder5xx, "https://generativelanguage.googleapis.com", objectMapper,
                TEST_API_KEY, "gemini-2.5-flash-lite", true, true, 500
        );
        server5xx.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent"))
                .andRespond(withServerError());
        AiProviderException err5xx = assertThrows(AiProviderException.class,
                () -> provider5xx.recommend(AiProviderRequest.forCatalog("Quero massa", "{}", 500)));
        assertEquals(FailureType.HTTP_5XX, err5xx.getFailure().type());
    }

    @Test
    void throwsUnavailableOnMalformedModelText() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        GeminiFlashLiteProvider provider = new GeminiFlashLiteProvider(
                builder, "https://generativelanguage.googleapis.com", objectMapper,
                TEST_API_KEY, "gemini-2.5-flash-lite", true, true, 500
        );

        String geminiPayload = objectMapper.writeValueAsString(Map.of(
                "candidates", List.of(Map.of(
                        "content", Map.of(
                                "parts", List.of(Map.of("text", "não é json"))
                        )
                ))
        ));

        server.expect(requestTo("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash-lite:generateContent"))
                .andRespond(withSuccess(geminiPayload, MediaType.APPLICATION_JSON));

        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> provider.recommend(AiProviderRequest.forCatalog("Quero massa", "{}", 500)));
        assertEquals(FailureType.MALFORMED_RESPONSE, ex.getFailure().type());
    }
}
