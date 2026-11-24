package com.borae.assistant;

import com.borae.assistant.AiProviderFailure.AiProviderException;
import com.borae.assistant.AiProviderFailure.FailureType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
        when(gemini.recommend(any())).thenReturn(geminiResp);

        AiProviderResponse result = orchestrator.recommend(sampleRequest);

        assertEquals(AiProviderId.GEMINI, result.provider());
        assertFalse(result.usedFallbackProvider());
        assertNull(result.primaryFailureCode());
        verify(gemini, times(1)).recommend(any());
        verify(grok, never()).recommend(any());
    }

    @Test
    void scenarioBAndI_geminiConsumesPartOfBudgetAndGrokReceivesCappedRemainingDeadline() {
        when(gemini.recommend(any())).thenAnswer(invocation -> {
            clock.addAndGet(12_000L); // Gemini consome 12s dos 15s globais
            throw new AiProviderException(AiProviderId.GEMINI, FailureType.TIMEOUT, "Timeout Gemini");
        });

        ArgumentCaptor<AiProviderRequest> grokRequestCaptor = ArgumentCaptor.forClass(AiProviderRequest.class);
        when(grok.recommend(grokRequestCaptor.capture())).thenReturn(AiProviderResponse.of(
                AiProviderId.GROK, "Sugestão via contingência.",
                List.of(new AiProviderResponse.RecommendationCandidate(PRODUCT_ID, "Disponível hoje.")),
                115, 38
        ));

        AiProviderResponse result = orchestrator.recommend(sampleRequest);

        InOrder order = inOrder(gemini, grok);
        order.verify(gemini, times(1)).recommend(any());
        order.verify(grok, times(1)).recommend(any());

        AiRequestDeadline sharedDeadline = grokRequestCaptor.getValue().deadline();
        assertNotNull(sharedDeadline);
        assertEquals(3_000L, sharedDeadline.remainingMillis());
        // Grok com timeout individual de 8s fica efetivamente limitado aos 3s restantes do deadline global
        assertEquals(3_000L, sharedDeadline.timeoutForProvider(8_000L));

        assertEquals(AiProviderId.GROK, result.provider());
        assertTrue(result.usedFallbackProvider());
        assertEquals("TIMEOUT", result.primaryFailureCode());
    }

    @Test
    void scenarioC_geminiHttp5xxAnd429TriggerFallbackToGrok() {
        when(gemini.recommend(any()))
                .thenThrow(new AiProviderException(AiProviderId.GEMINI, FailureType.HTTP_5XX, 503, "Erro 503"))
                .thenThrow(new AiProviderException(AiProviderId.GEMINI, FailureType.RATE_LIMITED, 429, "Erro 429"));
        when(grok.recommend(any())).thenReturn(
                AiProviderResponse.of(AiProviderId.GROK, "Resposta válida.", List.of(), 90, 25)
        );

        AiProviderResponse first = orchestrator.recommend(sampleRequest);
        AiProviderResponse second = orchestrator.recommend(sampleRequest);

        assertEquals(AiProviderId.GROK, first.provider());
        assertEquals("HTTP_5XX", first.primaryFailureCode());
        assertEquals(AiProviderId.GROK, second.provider());
        assertEquals("RATE_LIMITED", second.primaryFailureCode());
        verify(grok, times(2)).recommend(any());
    }

    @Test
    void scenarioD_geminiMalformedResponseWithTokensPreservesBothAttemptsInFallbackResponse() {
        when(gemini.recommend(any())).thenThrow(
                new AiProviderException(AiProviderId.GEMINI, FailureType.MALFORMED_RESPONSE, null,
                        "JSON inválido", 150, 20, List.of())
        );
        when(grok.recommend(any())).thenReturn(
                AiProviderResponse.of(AiProviderId.GROK, "Opção válida.",
                        List.of(new AiProviderResponse.RecommendationCandidate(PRODUCT_ID, "No cardápio.")),
                        180, 45)
        );

        AiProviderResponse result = orchestrator.recommend(sampleRequest);

        assertEquals(AiProviderId.GROK, result.provider());
        assertTrue(result.usedFallbackProvider());
        assertEquals("MALFORMED_RESPONSE", result.primaryFailureCode());
        assertEquals(2, result.attempts().size());
        assertEquals(AiProviderId.GEMINI, result.attempts().get(0).provider());
        assertEquals(150, result.attempts().get(0).inputTokens());
        assertEquals(20, result.attempts().get(0).outputTokens());
        assertEquals(AiProviderId.GROK, result.attempts().get(1).provider());
        assertEquals(180, result.attempts().get(1).inputTokens());
        assertEquals(45, result.attempts().get(1).outputTokens());
    }

    @Test
    void scenarioE_bothProvidersUnavailableFailsWithoutRetryLoop() {
        when(gemini.recommend(any())).thenThrow(
                new AiProviderException(AiProviderId.GEMINI, FailureType.HTTP_5XX, 503, "Gemini fora")
        );
        when(grok.recommend(any())).thenThrow(
                new AiProviderException(AiProviderId.GROK, FailureType.TIMEOUT, "Grok timeout")
        );

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> orchestrator.recommend(sampleRequest));

        assertEquals(AiProviderId.GROK, exception.getFailure().provider());
        assertEquals(FailureType.TIMEOUT, exception.getFailure().type());
        verify(gemini, times(1)).recommend(any());
        verify(grok, times(1)).recommend(any());
    }

    @Test
    void scenarioF_safetyRefusalNeverFallsBackToGrokAndDoesNotOpenCircuitBreaker() {
        when(gemini.recommend(any())).thenThrow(
                new AiProviderException(AiProviderId.GEMINI, FailureType.SAFETY_REFUSAL, "Recusa de segurança")
        );

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> orchestrator.recommend(sampleRequest));

        assertEquals(FailureType.SAFETY_REFUSAL, exception.getFailure().type());
        verify(gemini, times(1)).recommend(any());
        verify(grok, never()).recommend(any());
        assertEquals(AiProviderCircuitBreaker.State.CLOSED, orchestrator.primaryCircuitState());
    }

    @Test
    void respectsTotalTimeBudgetAndSkipsFallbackWhenBudgetExhausted() {
        when(gemini.recommend(any())).thenAnswer(invocation -> {
            clock.addAndGet(14_800L);
            throw new AiProviderException(AiProviderId.GEMINI, FailureType.TIMEOUT, "Timeout longo");
        });

        AiProviderException exception = assertThrows(AiProviderException.class,
                () -> orchestrator.recommend(sampleRequest));

        assertEquals(AiProviderId.GEMINI, exception.getFailure().provider());
        assertEquals(FailureType.TIMEOUT, exception.getFailure().type());
        verify(grok, never()).recommend(any());
    }

    @Test
    void expiredDeadlineBeforeCallPreventsBothGeminiAndGrok() {
        AiRequestDeadline expired = AiRequestDeadline.start(1_000L, 500L, clock::get);
        clock.addAndGet(1_200L);

        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> orchestrator.recommend(sampleRequest.withDeadline(expired)));

        assertEquals(FailureType.TIMEOUT, ex.getFailure().type());
        verify(gemini, never()).recommend(any());
        verify(grok, never()).recommend(any());
    }

    @Test
    void authAndConfigErrorsDoNotFallbackToGrokByDefault() {
        // 401, 403 e 400/404 não acionam Grok com a política padrão (AI_FALLBACK_ON_AUTH_OR_CONFIG_ERROR=false)
        assertFalse(AiProviderConfiguration.defaults().isFallbackOnAuthOrConfigError());

        for (int status : List.of(401, 403, 400, 404)) {
            reset(gemini, grok);
            orchestrator.resetCircuitBreakers();
            when(gemini.isAvailable()).thenReturn(true);
            when(gemini.recommend(any())).thenThrow(
                    new AiProviderException(AiProviderId.GEMINI, FailureType.AUTH_OR_CONFIG_ERROR, status, "Erro config/auth")
            );

            AiProviderException ex = assertThrows(AiProviderException.class,
                    () -> orchestrator.recommend(sampleRequest));
            assertEquals(FailureType.AUTH_OR_CONFIG_ERROR, ex.getFailure().type());
            verify(grok, never()).recommend(any());
        }
    }

    @Test
    void authAndConfigFallbackCanBeExplicitlyEnabledInConfiguration() {
        AiProviderConfiguration explicitOptIn = new AiProviderConfiguration(
                "gemini", "grok", true, true, 15_000L, 500, 3, 30_000L
        );
        AiProviderOrchestrator optInOrchestrator = new AiProviderOrchestrator(gemini, grok, explicitOptIn, clock::get);

        when(gemini.recommend(any())).thenThrow(
                new AiProviderException(AiProviderId.GEMINI, FailureType.AUTH_OR_CONFIG_ERROR, 401, "Chave inválida")
        );
        when(grok.recommend(any())).thenReturn(
                AiProviderResponse.of(AiProviderId.GROK, "Fallback autorizado.", List.of(), 90, 20)
        );

        AiProviderResponse response = optInOrchestrator.recommend(sampleRequest);
        assertEquals(AiProviderId.GROK, response.provider());
        assertTrue(response.usedFallbackProvider());
        assertEquals("AUTH_OR_CONFIG_ERROR", response.primaryFailureCode());
    }

    @Test
    void independentCircuitBreakersIsolateGeminiFailuresFromGrokAndRecoverAfterCooldown() {
        when(gemini.recommend(any())).thenThrow(
                new AiProviderException(AiProviderId.GEMINI, FailureType.HTTP_5XX, 502, "Falha temporária")
        );
        when(grok.recommend(any())).thenReturn(
                AiProviderResponse.of(AiProviderId.GROK, "Fallback Grok", List.of(), 80, 20)
        );

        for (int i = 0; i < 3; i++) {
            AiProviderResponse resp = orchestrator.recommend(sampleRequest);
            assertEquals(AiProviderId.GROK, resp.provider());
        }
        assertEquals(AiProviderCircuitBreaker.State.OPEN, orchestrator.primaryCircuitState());
        assertEquals(AiProviderCircuitBreaker.State.CLOSED, orchestrator.fallbackCircuitState());

        orchestrator.recommend(sampleRequest);
        verify(gemini, times(3)).recommend(any());
        verify(grok, times(4)).recommend(any());

        clock.addAndGet(31_000L);
        reset(gemini);
        when(gemini.isAvailable()).thenReturn(true);
        when(gemini.recommend(any())).thenReturn(
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
                "grok", "gemini", true, false, 15_000L, 500, 3, 30_000L
        ));
    }
}
