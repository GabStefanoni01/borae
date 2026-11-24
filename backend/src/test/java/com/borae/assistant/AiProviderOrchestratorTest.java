package com.borae.assistant;

import com.borae.assistant.AiProviderFailure.AiProviderException;
import com.borae.assistant.AiProviderFailure.FailureType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiProviderOrchestratorTest {

    private static final UUID PRODUCT_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    @Mock private AiProvider gemini;
    @Mock private AiProvider grok;

    private AtomicLong clock;
    private AiProviderOrchestrator orchestrator;
    private AiProviderRequest sampleRequest;

    @BeforeEach
    void setUp() {
        clock = new AtomicLong(1_000_000L);
        lenient().when(gemini.id()).thenReturn(AiProviderId.GEMINI);
        lenient().when(grok.id()).thenReturn(AiProviderId.GROK);
        lenient().when(gemini.isAvailable()).thenReturn(true);
        lenient().when(grok.isAvailable()).thenReturn(true);

        orchestrator = new AiProviderOrchestrator(gemini, grok, AiProviderConfiguration.defaults(), clock::get);
        sampleRequest = AiProviderRequest.forCatalog("Quero uma massa leve", "{\"restaurant\":\"Demo\",\"products\":[]}", 500);
    }

    @Test
    void scenarioAAndI_geminiWorkingReturnsPrimaryAndNeverCallsGrok() {
        AiProviderResponse geminiResp = AiProviderResponse.of(
                AiProviderId.GEMINI, "Sugiro o nhoque.",
                List.of(new AiProviderResponse.RecommendationCandidate(PRODUCT_ID, "Leve e artesanal.")),
                120, 40
        );
        when(gemini.recommend(sampleRequest)).thenReturn(geminiResp);

        AiProviderResponse result = orchestrator.recommend(sampleRequest);

        assertEquals(AiProviderId.GEMINI, result.provider());
        assertFalse(result.usedFallbackProvider());
        assertNull(result.primaryFailureCode());
        verify(gemini, times(1)).recommend(sampleRequest);
        verify(grok, never()).recommend(any());
    }

    @Test
    void scenarioBAndI_geminiTimeoutTriggersGrokOnceInStrictOrderWithSameRequest() {
        when(gemini.recommend(sampleRequest)).thenThrow(
                new AiProviderException(AiProviderId.GEMINI, FailureType.TIMEOUT, "Timeout Gemini")
        );
        AiProviderResponse grokResp = AiProviderResponse.of(
                AiProviderId.GROK, "Sugestão via contingência.",
                List.of(new AiProviderResponse.RecommendationCandidate(PRODUCT_ID, "Disponível hoje.")),
                115, 38
        );
        when(grok.recommend(sampleRequest)).thenReturn(grokResp);

        AiProviderResponse result = orchestrator.recommend(sampleRequest);

        InOrder order = inOrder(gemini, grok);
        order.verify(gemini, times(1)).recommend(sampleRequest);
        order.verify(grok, times(1)).recommend(sampleRequest);

        assertEquals(AiProviderId.GROK, result.provider());
        assertTrue(result.usedFallbackProvider());
        assertEquals("TIMEOUT", result.primaryFailureCode());
    }

    @Test
    void scenarioC_geminiHttp5xxAnd429TriggerFallbackToGrok() {
        when(gemini.recommend(sampleRequest))
                .thenThrow(new AiProviderException(AiProviderId.GEMINI, FailureType.HTTP_5XX, 503, "Erro 503"))
                .thenThrow(new AiProviderException(AiProviderId.GEMINI, FailureType.RATE_LIMITED, 429, "Erro 429"));
        when(grok.recommend(sampleRequest)).thenReturn(
                AiProviderResponse.of(AiProviderId.GROK, "Resposta válida.", List.of(), 90, 25)
        );

        AiProviderResponse first = orchestrator.recommend(sampleRequest);
        AiProviderResponse second = orchestrator.recommend(sampleRequest);

        assertEquals(AiProviderId.GROK, first.provider());
        assertEquals("HTTP_5XX", first.primaryFailureCode());
        assertEquals(AiProviderId.GROK, second.provider());
        assertEquals("RATE_LIMITED", second.primaryFailureCode());
        verify(grok, times(2)).recommend(sampleRequest);
    }

    @Test
    void scenarioD_geminiMalformedResponseTriggersFallbackToGrok() {
        when(gemini.recommend(sampleRequest)).thenThrow(
                new AiProviderException(AiProviderId.GEMINI, FailureType.MALFORMED_RESPONSE, "JSON inválido")
        );
        when(grok.recommend(sampleRequest)).thenReturn(
                AiProviderResponse.of(AiProviderId.GROK, "Opção válida.",
                        List.of(new AiProviderResponse.RecommendationCandidate(PRODUCT_ID, "No cardápio.")),
                        100, 30)
        );

        AiProviderResponse result = orchestrator.recommend(sampleRequest);

        assertEquals(AiProviderId.GROK, result.provider());
        assertTrue(result.usedFallbackProvider());
        assertEquals("MALFORMED_RESPONSE", result.primaryFailureCode());
    }

    @Test
    void scenarioE_bothProvidersUnavailableFailsWithoutRetryLoop() {
        when(gemini.recommend(sampleRequest)).thenThrow(
                new AiProviderException(AiProviderId.GEMINI, FailureType.HTTP_5XX, 503, "Gemini fora")
        );
        when(grok.recommend(sampleRequest)).thenThrow(
                new AiProviderException(AiProviderId.GROK, FailureType.TIMEOUT, "Grok timeout")
        );

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> orchestrator.recommend(sampleRequest));

        assertEquals(AiProviderId.GROK, exception.getFailure().provider());
        assertEquals(FailureType.TIMEOUT, exception.getFailure().type());
        verify(gemini, times(1)).recommend(sampleRequest);
        verify(grok, times(1)).recommend(sampleRequest);
    }

    @Test
    void scenarioF_safetyRefusalNeverFallsBackToGrokAndDoesNotOpenCircuitBreaker() {
        when(gemini.recommend(sampleRequest)).thenThrow(
                new AiProviderException(AiProviderId.GEMINI, FailureType.SAFETY_REFUSAL, "Recusa de segurança")
        );

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> orchestrator.recommend(sampleRequest));

        assertEquals(FailureType.SAFETY_REFUSAL, exception.getFailure().type());
        verify(gemini, times(1)).recommend(sampleRequest);
        verify(grok, never()).recommend(any());
        assertEquals(AiProviderCircuitBreaker.State.CLOSED, orchestrator.primaryCircuitState());
    }

    @Test
    void respectsTotalTimeBudgetAndSkipsFallbackWhenBudgetExhausted() {
        when(gemini.recommend(sampleRequest)).thenAnswer(invocation -> {
            clock.addAndGet(14_800L); // Consome quase todo o orçamento de 15.000 ms
            throw new AiProviderException(AiProviderId.GEMINI, FailureType.TIMEOUT, "Timeout longo");
        });

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> orchestrator.recommend(sampleRequest));

        assertEquals(AiProviderId.GEMINI, exception.getFailure().provider());
        assertEquals(FailureType.TIMEOUT, exception.getFailure().type());
        verify(grok, never()).recommend(any());
    }

    @Test
    void independentCircuitBreakersIsolateGeminiFailuresFromGrokAndRecoverAfterCooldown() {
        when(gemini.recommend(sampleRequest)).thenThrow(
                new AiProviderException(AiProviderId.GEMINI, FailureType.HTTP_5XX, 502, "Falha temporária")
        );
        when(grok.recommend(sampleRequest)).thenReturn(
                AiProviderResponse.of(AiProviderId.GROK, "Fallback Grok", List.of(), 80, 20)
        );

        // 3 falhas consecutivas abrem o circuit breaker do Gemini
        for (int i = 0; i < 3; i++) {
            AiProviderResponse resp = orchestrator.recommend(sampleRequest);
            assertEquals(AiProviderId.GROK, resp.provider());
        }
        assertEquals(AiProviderCircuitBreaker.State.OPEN, orchestrator.primaryCircuitState());
        assertEquals(AiProviderCircuitBreaker.State.CLOSED, orchestrator.fallbackCircuitState());

        // 4ª chamada: Gemini é pulado (circuit OPEN) e Grok responde diretamente
        orchestrator.recommend(sampleRequest);
        verify(gemini, times(3)).recommend(sampleRequest);
        verify(grok, times(4)).recommend(sampleRequest);

        // Após janela de 30s, Gemini entra em HALF_OPEN e volta a ser tentado primeiro
        clock.addAndGet(31_000L);
        reset(gemini);
        when(gemini.isAvailable()).thenReturn(true);
        when(gemini.recommend(sampleRequest)).thenReturn(
                AiProviderResponse.of(AiProviderId.GEMINI, "Gemini recuperado!", List.of(), 100, 30)
        );

        AiProviderResponse recovered = orchestrator.recommend(sampleRequest);
        assertEquals(AiProviderId.GEMINI, recovered.provider());
        assertFalse(recovered.usedFallbackProvider());
        assertEquals(AiProviderCircuitBreaker.State.CLOSED, orchestrator.primaryCircuitState());
    }

    @Test
    void configurationRejectsSettingGrokAheadOfGemini() {
        assertThrows(IllegalStateException.class, () -> new AiProviderConfiguration(
                "grok", "gemini", true, true, 15_000L, 500, 3, 30_000L
        ));
    }

    @Test
    void respectsFallbackOnAuthOrConfigPolicyFlag() {
        AiProviderConfiguration noAuthFallback = new AiProviderConfiguration(
                "gemini", "grok", true, false, 15_000L, 500, 3, 30_000L
        );
        AiProviderOrchestrator strictOrchestrator = new AiProviderOrchestrator(gemini, grok, noAuthFallback, clock::get);

        when(gemini.recommend(sampleRequest)).thenThrow(
                new AiProviderException(AiProviderId.GEMINI, FailureType.AUTH_OR_CONFIG_ERROR, 401, "Chave inválida")
        );

        assertThrows(AiProviderException.class, () -> strictOrchestrator.recommend(sampleRequest));
        verify(grok, never()).recommend(any());
    }
}
