package com.borae.assistant;

import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * {@link ClientHttpRequestFactory} que compartilha uma única instância de {@link HttpClient}
 * (sem criação de threads ou pools por chamada) e aplica o timeout efetivo de cada requisição
 * conforme o {@link AiRequestDeadline} ativo.
 *
 * <p>Internamente o {@link JdkClientHttpRequestFactory} do Spring define tanto o timeout nativo
 * do {@code java.net.http.HttpRequest} quanto o cancelamento do {@code CompletableFuture}
 * ({@code cancel(true)}), interrompendo o stream HTTP subjacente quando o prazo expira.</p>
 */
final class DeadlineAwareClientHttpRequestFactory implements ClientHttpRequestFactory {

    private static final ThreadLocal<Long> CURRENT_TIMEOUT_MS = new ThreadLocal<>();

    private final HttpClient sharedHttpClient;
    private final long defaultTimeoutMs;

    DeadlineAwareClientHttpRequestFactory(HttpClient sharedHttpClient, long defaultTimeoutMs) {
        this.sharedHttpClient = sharedHttpClient;
        this.defaultTimeoutMs = Math.max(1L, defaultTimeoutMs);
    }

    static DeadlineAwareClientHttpRequestFactory create(long connectTimeoutMs, long defaultReadTimeoutMs) {
        long safeConnect = Math.max(1L, connectTimeoutMs);
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(safeConnect))
                .build();
        return new DeadlineAwareClientHttpRequestFactory(client, defaultReadTimeoutMs);
    }

    static <T> T executeWithTimeout(long timeoutMs, java.util.function.Supplier<T> action) {
        Long previous = CURRENT_TIMEOUT_MS.get();
        CURRENT_TIMEOUT_MS.set(Math.max(1L, timeoutMs));
        try {
            return action.get();
        } finally {
            if (previous == null) {
                CURRENT_TIMEOUT_MS.remove();
            } else {
                CURRENT_TIMEOUT_MS.set(previous);
            }
        }
    }

    @Override
    public ClientHttpRequest createRequest(URI uri, HttpMethod httpMethod) throws IOException {
        Long scoped = CURRENT_TIMEOUT_MS.get();
        long effectiveMs = scoped != null ? Math.max(1L, scoped) : defaultTimeoutMs;
        JdkClientHttpRequestFactory delegate = new JdkClientHttpRequestFactory(sharedHttpClient);
        delegate.setReadTimeout(Duration.ofMillis(effectiveMs));
        return delegate.createRequest(uri, httpMethod);
    }
}
