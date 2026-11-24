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
                TEST_API_KEY, "grok-4.3", false, true, 500).isAvailable());
        assertFalse(new GrokAiProvider(builder, "https://api.x.ai", objectMapper,
                TEST_API_KEY, "grok-4.3", true, false, 500).isAvailable());
        assertFalse(new GrokAiProvider(builder, "https://api.x.ai", objectMapper,
                "", "grok-4.3", true, true, 500).isAvailable());
    }

    @Test
    void callsResponsesApiWithBearerHeaderStoreFalseAndStructuredOutputFormat() throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        GrokAiProvider provider = new GrokAiProvider(
                builder, "https://api.x.ai", objectMapper,
                TEST_API_KEY, "grok-4.3", true, true, 500
        );

        String modelJson = """
                {"answer":"O salmão grelhado é uma opção leve do cardápio.","recommendations":[{"productId":"22222222-2222-2222-2222-222222222222","reason":"Peixe grelhado com legumes frescos."}]}
                """;
        String xaiPayload = objectMapper.writeValueAsString(Map.of(
                "id", "resp_001",
                "status", "completed",
                "output", List.of(Map.of(
                        "type", "message",
                        "role", "assistant",
                        "content", List.of(Map.of(
                                "type", "output_text",
                                "text", modelJson
                        ))
                )),
                "usage", Map.of("input_tokens", 280, "output_tokens", 62)
        ));

        server.expect(requestTo("https://api.x.ai/v1/responses"))
                .andExpect(requestTo(not(containsString(TEST_API_KEY))))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer " + TEST_API_KEY))
                .andExpect(jsonPath("$.model").value("grok-4.3"))
                .andExpect(jsonPath("$.store").value(false))
                .andExpect(jsonPath("$.max_output_tokens").value(500))
                .andExpect(jsonPath("$.instructions").exists())
                .andExpect(jsonPath("$.input").exists())
                .andExpect(jsonPath("$.text.format.type").value("json_schema"))
                .andExpect(jsonPath("$.text.format.strict").value(true))
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
    void classifiesRefusalPartAndContentFilterAsSafetyRefusal() throws Exception {
        RestClient.Builder builderRefusal = RestClient.builder();
        MockRestServiceServer serverRefusal = MockRestServiceServer.bindTo(builderRefusal).build();
        GrokAiProvider providerRefusal = new GrokAiProvider(
                builderRefusal, "https://api.x.ai", objectMapper,
                TEST_API_KEY, "grok-4.3", true, true, 500
        );
        String refusalPayload = objectMapper.writeValueAsString(Map.of(
                "status", "completed",
                "output", List.of(Map.of(
                        "type", "message",
                        "content", List.of(Map.of("type", "refusal", "refusal", "Cannot comply."))
                )),
                "usage", Map.of("input_tokens", 110, "output_tokens", 12)
        ));
        serverRefusal.expect(requestTo("https://api.x.ai/v1/responses"))
                .andRespond(withSuccess(refusalPayload, MediaType.APPLICATION_JSON));
        AiProviderException refusalEx = assertThrows(AiProviderException.class,
                () -> providerRefusal.recommend(AiProviderRequest.forCatalog("Conteúdo bloqueado", "{}", 500)));
        assertEquals(FailureType.SAFETY_REFUSAL, refusalEx.getFailure().type());
        assertEquals(110, refusalEx.getFailure().inputTokens());
        assertEquals(12, refusalEx.getFailure().outputTokens());
        assertFalse(refusalEx.getMessage().contains(TEST_API_KEY));

        RestClient.Builder builderFilter = RestClient.builder();
        MockRestServiceServer serverFilter = MockRestServiceServer.bindTo(builderFilter).build();
        GrokAiProvider providerFilter = new GrokAiProvider(
                builderFilter, "https://api.x.ai", objectMapper,
                TEST_API_KEY, "grok-4.3", true, true, 500
        );
        String filterPayload = objectMapper.writeValueAsString(Map.of(
                "status", "incomplete",
                "incomplete_details", Map.of("reason", "content_filter"),
                "output", List.of()
        ));
        serverFilter.expect(requestTo("https://api.x.ai/v1/responses"))
                .andRespond(withSuccess(filterPayload, MediaType.APPLICATION_JSON));
        AiProviderException filterEx = assertThrows(AiProviderException.class,
                () -> providerFilter.recommend(AiProviderRequest.forCatalog("Conteúdo filtrado", "{}", 500)));
        assertEquals(FailureType.SAFETY_REFUSAL, filterEx.getFailure().type());
    }

    @Test
    void classifiesHttp400_401_403_429_5xxEmptyAndMalformedResponsesWithoutLeakingKey() throws Exception {
        assertHttpFailure(HttpStatus.BAD_REQUEST, FailureType.AUTH_OR_CONFIG_ERROR);
        assertHttpFailure(HttpStatus.UNAUTHORIZED, FailureType.AUTH_OR_CONFIG_ERROR);
        assertHttpFailure(HttpStatus.FORBIDDEN, FailureType.AUTH_OR_CONFIG_ERROR);
        assertHttpFailure(HttpStatus.TOO_MANY_REQUESTS, FailureType.RATE_LIMITED);
        assertHttpFailure(HttpStatus.INTERNAL_SERVER_ERROR, FailureType.HTTP_5XX);

        // Resposta vazia
        RestClient.Builder builderEmpty = RestClient.builder();
        MockRestServiceServer serverEmpty = MockRestServiceServer.bindTo(builderEmpty).build();
        GrokAiProvider providerEmpty = new GrokAiProvider(
                builderEmpty, "https://api.x.ai", objectMapper, TEST_API_KEY, "grok-4.3", true, true, 500
        );
        serverEmpty.expect(requestTo("https://api.x.ai/v1/responses"))
                .andRespond(withSuccess(objectMapper.writeValueAsString(Map.of("output", List.of())), MediaType.APPLICATION_JSON));
        AiProviderException errEmpty = assertThrows(AiProviderException.class,
                () -> providerEmpty.recommend(AiProviderRequest.forCatalog("Oi", "{}", 500)));
        assertEquals(FailureType.MALFORMED_RESPONSE, errEmpty.getFailure().type());

        // JSON malformado preserva tokens faturados
        RestClient.Builder builderBad = RestClient.builder();
        MockRestServiceServer serverBad = MockRestServiceServer.bindTo(builderBad).build();
        GrokAiProvider providerBad = new GrokAiProvider(
                builderBad, "https://api.x.ai", objectMapper, TEST_API_KEY, "grok-4.3", true, true, 500
        );
        String badPayload = objectMapper.writeValueAsString(Map.of(
                "output", List.of(Map.of(
                        "type", "message",
                        "content", List.of(Map.of("type", "output_text", "text", "not-json"))
                )),
                "usage", Map.of("input_tokens", 95, "output_tokens", 18)
        ));
        serverBad.expect(requestTo("https://api.x.ai/v1/responses"))
                .andRespond(withSuccess(badPayload, MediaType.APPLICATION_JSON));
        AiProviderException errBad = assertThrows(AiProviderException.class,
                () -> providerBad.recommend(AiProviderRequest.forCatalog("Oi", "{}", 500)));
        assertEquals(FailureType.MALFORMED_RESPONSE, errBad.getFailure().type());
        assertEquals(95, errBad.getFailure().inputTokens());
        assertEquals(18, errBad.getFailure().outputTokens());
    }

    private void assertHttpFailure(HttpStatus status, FailureType expectedType) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        GrokAiProvider provider = new GrokAiProvider(
                builder, "https://api.x.ai", objectMapper, TEST_API_KEY, "grok-4.3", true, true, 500
        );
        server.expect(requestTo("https://api.x.ai/v1/responses")).andRespond(withStatus(status));
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> provider.recommend(AiProviderRequest.forCatalog("Oi", "{}", 500)));
        assertEquals(expectedType, ex.getFailure().type());
        assertEquals(status.value(), ex.getFailure().httpStatus());
        assertFalse(ex.getMessage().contains(TEST_API_KEY));
    }
}
