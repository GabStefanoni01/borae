package com.borae.assistant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Issues short-lived, opaque browser sessions for anonymous public assistant usage.
 * The raw session identifier is never persisted; quota storage must use quotaKey().
 */
@Service
public class AnonymousAssistantSessionService {

    public static final String COOKIE_NAME = "BORA_ASSISTANT_SESSION";
    private static final Duration SESSION_TTL = Duration.ofHours(24);
    private static final String SIGNING_PURPOSE = "borae:assistant-session:v1:";
    private static final String QUOTA_PURPOSE = "borae:assistant-quota:v1:";

    private final byte[] signingKey;
    private final boolean secureCookie;
    private final String sameSite;
    private final Clock clock;
    private final SecureRandom secureRandom = new SecureRandom();

    public AnonymousAssistantSessionService(
            @Value("${app.assistant.session.secret:${security.jwt.secret}}") String secret,
            @Value("${app.assistant.session.cookie-secure:false}") boolean secureCookie,
            @Value("${app.assistant.session.cookie-same-site:Lax}") String sameSite
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("A chave de assinatura da sessão do assistente deve ser configurada.");
        }
        this.signingKey = deriveKey(secret);
        this.secureCookie = secureCookie;
        this.sameSite = normalizeSameSite(sameSite);
        this.clock = Clock.systemUTC();
    }

    public IssuedSession issue() {
        byte[] random = new byte[32];
        secureRandom.nextBytes(random);
        String sessionId = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        Instant expiresAt = clock.instant().plus(SESSION_TTL);
        String payload = sessionId + "." + expiresAt.getEpochSecond();
        String value = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8))
                + "." + sign(payload);

        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite(sameSite)
                .path("/api/v1/public")
                .maxAge(SESSION_TTL)
                .build();

        return new IssuedSession(cookie, expiresAt);
    }

    /**
     * Returns a stable one-way identifier for daily quota keys, or null for an invalid cookie.
     * Callers must not log or persist the cookie or the decoded session identifier.
     */
    public String quotaKey(String cookieValue) {
        SessionClaims claims = validate(cookieValue);
        return claims == null ? null : hmac(QUOTA_PURPOSE + claims.sessionId());
    }

    public boolean isValid(String cookieValue) {
        return validate(cookieValue) != null;
    }

    private SessionClaims validate(String cookieValue) {
        if (cookieValue == null || cookieValue.length() > 512) return null;
        try {
            int separator = cookieValue.lastIndexOf('.');
            if (separator <= 0 || separator == cookieValue.length() - 1) return null;
            String encodedPayload = cookieValue.substring(0, separator);
            String suppliedSignature = cookieValue.substring(separator + 1);
            String payload = new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(sign(payload).getBytes(StandardCharsets.US_ASCII),
                    suppliedSignature.getBytes(StandardCharsets.US_ASCII))) return null;

            int claimsSeparator = payload.lastIndexOf('.');
            if (claimsSeparator <= 0) return null;
            String sessionId = payload.substring(0, claimsSeparator);
            long expiresEpoch = Long.parseLong(payload.substring(claimsSeparator + 1));
            if (sessionId.length() < 40 || expiresEpoch <= clock.instant().getEpochSecond()) return null;
            return new SessionClaims(sessionId, Instant.ofEpochSecond(expiresEpoch));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String sign(String payload) {
        return hmac(SIGNING_PURPOSE + payload);
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível assinar a sessão anônima.", exception);
        }
    }

    private static byte[] deriveKey(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest((SIGNING_PURPOSE + secret).getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException("Não foi possível derivar a chave da sessão.", exception);
        }
    }

    private static String normalizeSameSite(String value) {
        if ("Strict".equalsIgnoreCase(value)) return "Strict";
        if ("None".equalsIgnoreCase(value)) return "None";
        return "Lax";
    }

    private record SessionClaims(String sessionId, Instant expiresAt) {}
    public record IssuedSession(ResponseCookie cookie, Instant expiresAt) {}
}
