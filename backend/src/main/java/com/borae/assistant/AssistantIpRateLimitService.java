package com.borae.assistant;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

/**
 * Distributed burst protection for the public assistant endpoint.
 * Only an HMAC of the client address is persisted; raw addresses are never stored or logged.
 */
@Service
public class AssistantIpRateLimitService {

    public static final int REQUESTS_PER_MINUTE = 30;
    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final String HMAC_PURPOSE = "borae:assistant-ip-rate-limit:v1:";

    private static final String CONSUME_SQL = """
            INSERT INTO assistant_ip_rate_limit (window_start, ip_hash, request_count, updated_at)
            VALUES (?, ?, 1, NOW())
            ON CONFLICT (window_start, ip_hash)
            DO UPDATE SET request_count = assistant_ip_rate_limit.request_count + 1, updated_at = NOW()
            WHERE assistant_ip_rate_limit.request_count < ?
            RETURNING request_count
            """;

    private final JdbcTemplate jdbcTemplate;
    private final byte[] hmacKey;
    private final boolean trustForwardedHeaders;

    public AssistantIpRateLimitService(
            JdbcTemplate jdbcTemplate,
            @Value("${app.assistant.rate-limit.secret:${app.assistant.session.secret}}") String secret,
            @Value("${app.assistant.rate-limit.trust-forwarded-headers:false}") boolean trustForwardedHeaders
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("A chave de pseudonimização do rate limit deve ser configurada.");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.hmacKey = deriveKey(secret);
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    @Transactional
    public void checkAndConsume(HttpServletRequest request) {
        String clientAddress = resolveClientAddress(request);
        String ipHash = hashAddress(clientAddress);
        Instant now = Instant.now();
        Instant windowStart = now.truncatedTo(ChronoUnit.MINUTES);

        // Keep only a short operational window; old pseudonymous identifiers are purged on requests.
        jdbcTemplate.update("DELETE FROM assistant_ip_rate_limit WHERE window_start < ?",
                Timestamp.from(now.minus(Duration.ofHours(2))));

        var consumed = jdbcTemplate.query(
                CONSUME_SQL,
                (resultSet, rowNumber) -> resultSet.getInt(1),
                Timestamp.from(windowStart), ipHash, REQUESTS_PER_MINUTE
        );
        if (consumed.isEmpty()) {
            long retryAfter = Math.max(1, WINDOW.minusMillis(
                    Math.floorMod(now.toEpochMilli(), WINDOW.toMillis())).toSeconds());
            throw new AssistantIpRateLimitExceededException(retryAfter);
        }
    }

    private String resolveClientAddress(HttpServletRequest request) {
        if (trustForwardedHeaders) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (forwardedFor != null && !forwardedFor.isBlank()) {
                String firstAddress = forwardedFor.split(",", 2)[0].trim();
                String normalized = normalizeAddress(firstAddress);
                if (normalized != null) return normalized;
            }
        }

        String remoteAddress = normalizeAddress(request.getRemoteAddr());
        return remoteAddress == null ? "unknown" : remoteAddress;
    }

    private String normalizeAddress(String candidate) {
        if (candidate == null || candidate.isBlank()) return null;
        String value = candidate.trim();

        if (value.startsWith("[") && value.contains("]")) {
            value = value.substring(1, value.indexOf(']'));
        } else if (value.matches("\\d{1,3}(?:\\.\\d{1,3}){3}:\\d{1,5}")) {
            value = value.substring(0, value.lastIndexOf(':'));
        }

        // Restrict characters before parsing so hostnames cannot trigger DNS lookups.
        if (!value.matches("[0-9a-fA-F:.]+")) return null;
        try {
            return InetAddress.getByName(value).getHostAddress();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String hashAddress(String address) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            byte[] digest = mac.doFinal((HMAC_PURPOSE + address).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível gerar a chave pseudônima do rate limit.", exception);
        }
    }

    private static byte[] deriveKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest((HMAC_PURPOSE + secret).getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível derivar a chave do rate limit.", exception);
        }
    }

    public static class AssistantIpRateLimitExceededException extends RuntimeException {
        private final long retryAfterSeconds;

        public AssistantIpRateLimitExceededException(long retryAfterSeconds) {
            super("Muitas solicitações ao assistente a partir desta origem.");
            this.retryAfterSeconds = retryAfterSeconds;
        }

        public long getRetryAfterSeconds() {
            return retryAfterSeconds;
        }
    }
}
