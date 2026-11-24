package com.borae.assistant;

import com.borae.assistant.AiProviderFailure.AiProviderException;
import com.borae.assistant.AiProviderFailure.FailureType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes determinísticos do {@link AiRequestDeadline} e validação real de timeout/cancelamento
 * HTTP contra um servidor local bloqueante (sem chamadas externas).
 */
class AiRequestDeadlineAndCancellationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void capsProviderTimeoutToRemainingGlobalBudgetAndRespectsSmallerIndividualTimeout() {
        AtomicLong clock = new AtomicLong(10_000L);
        AiRequestDeadline deadline = AiRequestDeadline.start(15_000L, 500L, clock::get);

        // Timeout individual menor que o global restante -> respeita o individual
        assertEquals(8_000L, deadline.timeoutForProvider(8_000L));
        assertTrue(deadline.hasOperationalBudget());
        assertFalse(deadline.isExpired());

        // Simula Gemini consumindo 12.000 ms -> restam apenas 3.000 ms para o Grok (mesmo com timeout individual de 8.000 ms)
        clock.addAndGet(12_000L);
        assertEquals(3_000L, deadline.remainingMillis());
        assertEquals(3_000L, deadline.timeoutForProvider(8_000L));
        assertTrue(deadline.hasOperationalBudget());

        // Restam 300 ms (< mínimo operacional de 500 ms) -> não permite fallback
        clock.addAndGet(2_700L);
        assertEquals(300L, deadline.remainingMillis());
        assertFalse(deadline.hasOperationalBudget());
        assertFalse(deadline.isExpired());

        // Deadline expirado -> timeout efetivo = 0
        clock.addAndGet(300L);
        assertTrue(deadline.isExpired());
        assertEquals(0L, deadline.timeoutForProvider(8_000L));
    }

    @Test
    void expiredDeadlinePreventsHttpExecutionInBothAdapters() {
        AtomicLong clock = new AtomicLong(1_000L);
        AiRequestDeadline expired = AiRequestDeadline.start(1_000L, 500L, clock::get);
        clock.addAndGet(1_500L);

        GeminiFlashLiteProvider gemini = new GeminiFlashLiteProvider(
                RestClient.builder(), "http://127.0.0.1:1", objectMapper,
                "test-key", "gemini-2.5-flash-lite", true, true, 8_000L, 500
        );
        GrokAiProvider grok = new GrokAiProvider(
                RestClient.builder(), "http://127.0.0.1:1", objectMapper,
                "test-key", "grok-4.3", true, true, 8_000L, 500
        );

        AiProviderRequest req = AiProviderRequest.forCatalog("Quero massa", "{}", 500).withDeadline(expired);

        AiProviderException geminiEx = assertThrows(AiProviderException.class, () -> gemini.recommend(req));
        assertEquals(FailureType.TIMEOUT, geminiEx.getFailure().type());

        AiProviderException grokEx = assertThrows(AiProviderException.class, () -> grok.recommend(req));
        assertEquals(FailureType.TIMEOUT, grokEx.getFailure().type());
    }

    @Test
    void realLocalHttpCallsRespectEffectiveRemainingDeadlineWithoutWaitingForFullIndividualTimeout() throws Exception {
        CountDownLatch releaseServer = new CountDownLatch(1);
        AtomicInteger grokCalls = new AtomicInteger(0);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        try {
            // Gemini responde 503 rapidamente (~20ms)
            server.createContext("/v1beta/models/gemini-2.5-flash-lite:generateContent", exchange -> {
                byte[] body = "{\"error\":\"unavailable\"}".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(503, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            // Grok bloqueia por até 8s simulando lentidão extrema do upstream
            server.createContext("/v1/responses", exchange -> {
                grokCalls.incrementAndGet();
                try {
                    releaseServer.await(8, TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    exchange.close();
                }
            });
            server.start();

            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            // Timeout individual alto (8.000 ms), mas deadline global curto (900 ms)
            GeminiFlashLiteProvider gemini = GeminiFlashLiteProvider.withDeadlineAwareHttpClient(
                    baseUrl, objectMapper, "synthetic-gemini-key", "gemini-2.5-flash-lite",
                    true, true, 8_000L, 500
            );
            GrokAiProvider grok = GrokAiProvider.withDeadlineAwareHttpClient(
                    baseUrl, objectMapper, "synthetic-grok-key", "grok-4.3",
                    true, true, 8_000L, 500
            );

            AiProviderConfiguration config = new AiProviderConfiguration(
                    "gemini", "grok", true, false, 1_500L, 100L, 500, 3, 30_000L
            );
            AiProviderOrchestrator orchestrator = new AiProviderOrchestrator(gemini, grok, config);

            AiRequestDeadline tightDeadline = AiRequestDeadline.start(900L, 100L, System::currentTimeMillis);
            AiProviderRequest request = AiProviderRequest.forCatalog("Sugestão?", "{}", 500)
                    .withDeadline(tightDeadline);

            long startNs = System.nanoTime();
            AiProviderException exception = assertThrows(AiProviderException.class,
                    () -> orchestrator.recommend(request));
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs);

            assertEquals(AiProviderId.GROK, exception.getFailure().provider());
            assertEquals(FailureType.TIMEOUT, exception.getFailure().type());
            assertEquals(1, grokCalls.get());
            // Prova que o timeout efetivo do Grok foi limitado ao deadline global (~900ms) e NÃO aos 8.000ms individuais
            assertTrue(elapsedMs < 3_500L, "Esperado cancelamento próximo ao deadline de 900ms, mas levou " + elapsedMs + "ms");
        } finally {
            releaseServer.countDown();
            server.stop(0);
        }
    }
}
