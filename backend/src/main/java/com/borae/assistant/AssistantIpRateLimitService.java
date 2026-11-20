package com.borae.assistant;

import com.borae.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

/**
 * Distributed burst protection for the public assistant endpoints.
 * Delegates client IP resolution to the fail-closed {@link ClientIpResolver} and persists
 * only an HMAC-SHA256 hash of the client address; raw addresses are never stored or logged.
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
    private final ClientIpResolver clientIpResolver;
    private final byte[] hmacKey;

    @Autowired
    public AssistantIpRateLimitService(
            JdbcTemplate jdbcTemplate,
            ClientIpResolver clientIpResolver,
            @Value("${app.assistant.rate-limit.secret:${app.assistant.session.secret:${security.jwt.secret:}}}") String secret
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("A chave de pseudonimização do rate limit deve ser configurada.");
        }
        this.jdbcTemplate = jdbcTemplate;
        this.clientIpResolver = clientIpResolver;
        this.hmacKey = deriveKey(secret);
    }

    @Transactional
    public void checkAndConsume(HttpServletRequest request) {
        String clientAddress = clientIpResolver.resolve(request);
        String ipHash = hashAddress(clientAddress);
        Instant now = Instant.now();
        Instant windowStart = now.truncatedTo(ChronoUnit.MINUTES);

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

    @Scheduled(cron = "0 */15 * * * *")
    @Transactional
    public void purgeExpiredBuckets() {
        jdbcTemplate.update("DELETE FROM assistant_ip_rate_limit WHERE window_start < ?",
                Timestamp.from(Instant.now().minus(Duration.ofHours(2))));
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
