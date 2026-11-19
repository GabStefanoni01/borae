package com.borae.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;
    private static final int MIN_DISTINCT_BYTES = 8;
    private static final int MAX_TOKEN_LENGTH = 4096;
    private static final long MAX_EXPIRATION_MS = 7L * 24 * 60 * 60 * 1000;
    private static final String DEFAULT_ISSUER = "borae-api";

    private static final Set<String> FORBIDDEN_SECRETS = Set.of(
            "0123456789abcdef0123456789abcdef",
            "mdeymzq1njc4owfiy2rlzjaxmjm0nty3odlhymnkzwy=",
            "change-me",
            "changeme",
            "default-secret",
            "jwt-secret",
            "secret"
    );

    private final SecretKey signingKey;
    private final long expirationMs;
    private final String issuer;

    public JwtService(String secret, long expirationMs) {
        this(secret, expirationMs, DEFAULT_ISSUER);
    }

    @Autowired
    public JwtService(
            @Value("${security.jwt.secret:}") String secret,
            @Value("${security.jwt.expiration-ms:900000}") long expirationMs,
            @Value("${security.jwt.issuer:" + DEFAULT_ISSUER + "}") String issuer
    ) {
        this.signingKey = validateAndCreateSigningKey(secret);
        if (expirationMs <= 0 || expirationMs > MAX_EXPIRATION_MS) {
            throw new IllegalStateException("A configuração security.jwt.expiration-ms deve ser positiva e menor ou igual a 7 dias.");
        }
        this.expirationMs = expirationMs;
        this.issuer = (issuer == null || issuer.isBlank()) ? DEFAULT_ISSUER : issuer.trim();
    }

    public String generateToken(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("O subject do token JWT é obrigatório.");
        }
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subject.trim())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String extractSubject(String token) {
        if (token == null || token.isBlank() || token.length() > MAX_TOKEN_LENGTH) {
            throw new JwtException("Token JWT ausente ou inválido.");
        }
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        if (claims.getExpiration() == null) {
            throw new JwtException("Token JWT sem data de expiração.");
        }
        String tokenIssuer = claims.getIssuer();
        if (tokenIssuer != null && !issuer.equals(tokenIssuer)) {
            throw new JwtException("Emissor do token JWT inválido.");
        }
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new JwtException("Token JWT sem subject válido.");
        }
        return subject;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public String getIssuer() {
        return issuer;
    }

    private static SecretKey validateAndCreateSigningKey(String rawSecret) {
        if (rawSecret == null || rawSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET ausente. Configure uma chave Base64 criptograficamente segura com pelo menos 32 bytes.");
        }
        String trimmed = rawSecret.trim();
        if (FORBIDDEN_SECRETS.contains(trimmed.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException(
                    "JWT_SECRET utiliza um valor padrão ou previsível proibido. Configure um segredo exclusivo e seguro.");
        }

        byte[] decoded;
        try {
            decoded = Decoders.BASE64.decode(trimmed);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("JWT_SECRET deve estar codificado em Base64 válido.");
        }

        if (decoded == null || decoded.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET inválido: a chave decodificada deve possuir no mínimo 32 bytes (256 bits).");
        }

        String decodedText = new String(decoded, StandardCharsets.UTF_8).trim().toLowerCase(Locale.ROOT);
        if (FORBIDDEN_SECRETS.contains(decodedText) || decodedText.contains("0123456789abcdef0123456789abcdef")
                || decodedText.contains("change-me")) {
            throw new IllegalStateException(
                    "JWT_SECRET contém um segredo padrão ou previsível conhecido. Configure uma chave aleatória segura.");
        }

        Set<Byte> distinctBytes = new HashSet<>();
        for (byte b : decoded) {
            distinctBytes.add(b);
        }
        if (distinctBytes.size() < MIN_DISTINCT_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET possui baixa entropia criptográfica. Gere uma chave aleatória com alta entropia.");
        }

        return Keys.hmacShaKeyFor(decoded);
    }
}
