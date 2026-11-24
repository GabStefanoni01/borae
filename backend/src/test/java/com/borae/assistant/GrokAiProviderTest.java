package com.borae.assistant;

import com.borae.assistant.AiProviderFailure.AiProviderException;
import com.borae.assistant.AiProviderFailure.FailureType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
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

class GrokAiProviderTest {

    private static final String TEST_API_KEY = "test-only-synthetic-grok-key-never-real";
    private static final UUID PRODUCT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void reportsUnavailableWhenDisabledOrMissingApprovalOrKey() {
        RestClient.Builder builder = RestClient.builder();
        assertFalse(new GrokAiProvider(builder, "https://api.x.ai", objectMapper,
                TEST_API_KEY, "grok-3-mini", false, true, 500).isAvailable());
        assertFalse(new GrokAiProvider(builder, "https://api.x.ai", objectMapper,
                TEST_API_KEY, "grok-3-mini", true, false, 500).isAvailable());
        assertFalse(new GrokAiProvider(builder, "https://api.x.ai", objectMapper,
                "", "grok-3-mini", true, true, 500).isAvailable());
    }

    @Test
    void callsChatCompletionsWithBearerHeaderAndParsesNormalizedResponse() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        GrokAiProvider provider = new GrokAiProvider(
                builder, "https://api.x.ai", objectMapper,
                TEST_API_KEY, "grok-3-mini", true, true, 500
        );

        String modelJson = """
                {"answer":"O salmão grelhado é uma opção leve do cardápio.","recommendations":[{"productId":"22222222-2222-2222-2222-222222222222","reason":"Peixe grelhado com legumes frescos."}]}
                """;
        String xaiPayload = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of(
                        "finish_reason", "stop",
                        "message", Map.of("role", "assistant", "content", modelJson)
                )),
                "usage", Map.of("prompt_tokens", 280, "completion_tokens", 62)
        ));

        server.expect(requestTo("https://api.x.ai/v1/chat/completions"))
                .andExpect(requestTo(not(containsString(TEST_API_KEY))))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
                .andExpect(jsonPath("$.model").value("grok-3-mini"))
                .andExpect(jsonPath("$.max_tokens").value(500))
                .andExpect(jsonPath("$.response_format.type").value("json_schema"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].role").value("user"))
                .andRespond(withSuccess(xaiPayload, MediaType.APPLICATION_JSON));

        AiProviderResponse response = provider.recommend(
                AiProviderRequest.forCatalog("Quero algo leve", "{\"products\":[]}", 500)
        );

        server.verify();
        assertEquals(AiProviderId.GROK, response.provider());
        assertEquals("O salmão grelhado é uma opção leve do cardápio.", response.answer());
        assertEquals(1, response.recommendations().size());
        assertEquals(PRODUCT_ID, response.recommendations().getFirst().productId());
        assertEquals(280, response.inputTokens());
        assertEquals(62, response.outputTokens());
    }

    @Test
    void classifiesContentFilterAsSafetyRefusal() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        GrokAiProvider provider = new GrokAiProvider(
                builder, "https://api.x.ai", objectMapper,
                TEST_API_KEY, "grok-3-mini", true, true, 500
        );

        String xaiPayload = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of(
                        "finish_reason", "content_filter",
                        "message", Map.of("role", "assistant", "content", "")
                ))
        ));

        server.expect(requestTo("https://api.x.ai/v1/chat/completions"))
                .andRespond(withSuccess(xaiPayload, MediaType.APPLICATION_JSON));

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> provider.recommend(AiProviderRequest.forCatalog("Conteúdo bloqueado", "{}", 500)));

        assertEquals(FailureType.SAFETY_REFUSAL, exception.getFailure().type());
        assertFalse(exception.getMessage().contains(TEST_API_KEY));
    }

    @Test
    void classifiesHttp5xxRateLimitAuthAndMalformedResponsesWithoutLeakingKey() throws Exception {
        RestClient.Builder builder5xx = RestClient.builder();
        MockRestServiceServer server5xx = MockRestServiceServer.bindTo(builder5xx).build();
        GrokAiProvider provider5xx = new GrokAiProvider(
                builder5xx, "https://api.x.ai", objectMapper, TEST_API_KEY, "grok-3-mini", true, true, 500
        );
        server5xx.expect(requestTo("https://api.x.ai/v1/chat/completions")).andRespond(withServerError());
        AiProviderException err5xx = assertThrows(AiProviderException.class,
                () -> provider5xx.recommend(AiProviderRequest.forCatalog("Oi", "{}", 500)));
        assertEquals(FailureType.HTTP_5XX, err5xx.getFailure().type());
        assertFalse(err5xx.getMessage().contains(TEST_API_KEY));

        RestClient.Builder builder429 = RestClient.builder();
        MockRestServiceServer server429 = MockRestServiceServer.bindTo(builder429).build();
        GrokAiProvider provider429 = new GrokAiProvider(
                builder429, "https://api.x.ai", objectMapper, TEST_API_KEY, "grok-3-mini", true, true, 500
        );
        server429.expect(requestTo("https://api.x.ai/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        AiProviderException err429 = assertThrows(AiProviderException.class,
                () -> provider429.recommend(AiProviderRequest.forCatalog("Oi", "{}", 500)));
        assertEquals(FailureType.RATE_LIMITED, err429.getFailure().type());

        RestClient.Builder builder401 = RestClient.builder();
        MockRestServiceServer server401 = MockRestServiceServer.bindTo(builder401).build();
        GrokAiProvider provider401 = new GrokAiProvider(
                builder401, "https://api.x.ai", objectMapper, TEST_API_KEY, "grok-3-mini", true, true, 500
        );
        server401.expect(requestTo("https://api.x.ai/v1/chat/completions"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        AiProviderException err401 = assertThrows(AiProviderException.class,
                () -> provider401.recommend(AiProviderRequest.forCatalog("Oi", "{}", 500)));
        assertEquals(FailureType.AUTH_OR_CONFIG_ERROR, err401.getFailure().type());
        assertFalse(err401.getMessage().contains(TEST_API_KEY));

        RestClient.Builder builderBad = RestClient.builder();
        MockRestServiceServer serverBad = MockRestServiceServer.bindTo(builderBad).build();
        GrokAiProvider providerBad = new GrokAiProvider(
                builderBad, "https://api.x.ai", objectMapper, TEST_API_KEY, "grok-3-mini", true, true, 500
        );
        String badPayload = objectMapper.writeValueAsString(Map.of(
                "choices", List.of(Map.of("finish_reason", "stop", "message", Map.of("content", "not-json")))
        ));
        serverBad.expect(requestTo("https://api.x.ai/v1/chat/completions"))
                .andRespond(withSuccess(badPayload, MediaType.APPLICATION_JSON));
        AiProviderException errBad = assertThrows(AiProviderException.class,
                () -> providerBad.recommend(AiProviderRequest.forCatalog("Oi", "{}", 500)));
        assertEquals(FailureType.MALFORMED_RESPONSE, errBad.getFailure().type());
    }
}
