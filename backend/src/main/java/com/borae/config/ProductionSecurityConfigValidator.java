package com.borae.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Validador de segurança das configurações de inicialização em produção.
 *
 * <p>Garante falha fechada (fail-closed) caso variáveis obrigatórias estejam ausentes,
 * incompletas, compartilhadas indevidamente ou utilizem valores padrão inseguros no perfil de produção.</p>
 */
@Component
public class ProductionSecurityConfigValidator {

    private static final int MIN_SECRET_BYTES = 32;

    private static final Set<String> INSECURE_PASSWORDS = Set.of(
            "change-me",
            "changeme",
            "password",
            "postgres",
            "123456",
            "12345678",
            "admin",
            "borae",
            "root"
    );

    private static final Set<String> INSECURE_SECRETS = Set.of(
            "0123456789abcdef0123456789abcdef",
            "mdeymzq1njc4owfiy2rlzjaxmjm0nty3odlhymnkzwy=",
            "change-me",
            "changeme",
            "default-secret",
            "jwt-secret",
            "secret",
            "assistant-secret"
    );

    private final Environment environment;
    private final String datasourceUrl;
    private final String datasourceUsername;
    private final String datasourcePassword;
    private final String corsAllowedOrigins;
    private final String publicRestaurantUrlPrefix;
    private final String jwtSecret;
    private final String assistantSessionSecret;
    private final String assistantRateLimitSecret;
    private final boolean assistantSessionCookieSecure;

    @Autowired
    public ProductionSecurityConfigValidator(
            Environment environment,
            @Value("${spring.datasource.url:}") String datasourceUrl,
            @Value("${spring.datasource.username:}") String datasourceUsername,
            @Value("${spring.datasource.password:}") String datasourcePassword,
            @Value("${app.cors.allowed-origins:}") String corsAllowedOrigins,
            @Value("${app.public-restaurant-url-prefix:}") String publicRestaurantUrlPrefix,
            @Value("${security.jwt.secret:}") String jwtSecret,
            @Value("${app.assistant.session.secret:}") String assistantSessionSecret,
            @Value("${app.assistant.rate-limit.secret:}") String assistantRateLimitSecret,
            @Value("${app.assistant.session.cookie-secure:false}") boolean assistantSessionCookieSecure
    ) {
        this.environment = environment;
        this.datasourceUrl = datasourceUrl;
        this.datasourceUsername = datasourceUsername;
        this.datasourcePassword = datasourcePassword;
        this.corsAllowedOrigins = corsAllowedOrigins;
        this.publicRestaurantUrlPrefix = publicRestaurantUrlPrefix;
        this.jwtSecret = jwtSecret;
        this.assistantSessionSecret = assistantSessionSecret;
        this.assistantRateLimitSecret = assistantRateLimitSecret;
        this.assistantSessionCookieSecure = assistantSessionCookieSecure;
    }

    @PostConstruct
    public void validateOnStartup() {
        if (isProductionProfileActive()) {
            validateProductionConfiguration();
        }
    }

    public boolean isProductionProfileActive() {
        if (environment == null) {
            return false;
        }
        return Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(profile -> "prod".equals(profile) || "production".equals(profile));
    }

    public void validateProductionConfiguration() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("Configuração de produção inválida: JWT_SECRET é obrigatório.");
        }

        validateIndependentProductionSecret(
                assistantSessionSecret,
                "ASSISTANT_SESSION_SECRET",
                jwtSecret,
                "JWT_SECRET"
        );
        validateIndependentProductionSecret(
                assistantRateLimitSecret,
                "ASSISTANT_RATE_LIMIT_SECRET",
                jwtSecret,
                "JWT_SECRET"
        );
        if (assistantRateLimitSecret.trim().equals(assistantSessionSecret.trim())) {
            throw new IllegalStateException(
                    "Configuração de produção inválida: ASSISTANT_RATE_LIMIT_SECRET deve ser independente de ASSISTANT_SESSION_SECRET.");
        }
        if (!assistantSessionCookieSecure) {
            throw new IllegalStateException(
                    "Configuração de produção inválida: ASSISTANT_SESSION_COOKIE_SECURE deve ser true em produção.");
        }

        if (datasourceUrl == null || datasourceUrl.isBlank()
                || !datasourceUrl.trim().toLowerCase(Locale.ROOT).startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException(
                    "Configuração de produção inválida: DB_URL deve ser uma URL JDBC PostgreSQL válida.");
        }
        String lowerDbUrl = datasourceUrl.trim().toLowerCase(Locale.ROOT);
        if (lowerDbUrl.contains("://localhost") || lowerDbUrl.contains("://127.0.0.1")) {
            throw new IllegalStateException(
                    "Configuração de produção inválida: DB_URL não pode apontar para localhost em produção.");
        }

        if (datasourceUsername == null || datasourceUsername.isBlank()) {
            throw new IllegalStateException("Configuração de produção inválida: DB_USERNAME é obrigatório.");
        }

        if (datasourcePassword == null || datasourcePassword.isBlank()) {
            throw new IllegalStateException("Configuração de produção inválida: DB_PASSWORD é obrigatório.");
        }
        String normalizedPassword = datasourcePassword.trim().toLowerCase(Locale.ROOT);
        if (normalizedPassword.length() < 8 || INSECURE_PASSWORDS.contains(normalizedPassword)) {
            throw new IllegalStateException(
                    "Configuração de produção inválida: DB_PASSWORD utiliza um valor padrão ou inseguro.");
        }

        if (corsAllowedOrigins == null || corsAllowedOrigins.isBlank()) {
            throw new IllegalStateException(
                    "Configuração de produção inválida: CORS_ALLOWED_ORIGINS deve listar explicitamente as origens HTTPS autorizadas.");
        }
        List<String> origins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
        if (origins.isEmpty()) {
            throw new IllegalStateException(
                    "Configuração de produção inválida: CORS_ALLOWED_ORIGINS não contém nenhuma origem válida.");
        }
        for (String origin : origins) {
            if ("*".equals(origin) || origin.contains("*")) {
                throw new IllegalStateException(
                        "Configuração de produção inválida: CORS_ALLOWED_ORIGINS não permite wildcard (*).");
            }
            String lowerOrigin = origin.toLowerCase(Locale.ROOT);
            if (!lowerOrigin.startsWith("https://")
                    || lowerOrigin.contains("localhost")
                    || lowerOrigin.contains("127.0.0.1")) {
                throw new IllegalStateException(
                        "Configuração de produção inválida: todas as origens em CORS_ALLOWED_ORIGINS devem utilizar HTTPS e domínio de produção.");
            }
        }

        if (publicRestaurantUrlPrefix == null || publicRestaurantUrlPrefix.isBlank()) {
            throw new IllegalStateException(
                    "Configuração de produção inválida: PUBLIC_RESTAURANT_URL_PREFIX é obrigatório.");
        }
        URI publicUri;
        try {
            publicUri = URI.create(publicRestaurantUrlPrefix.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Configuração de produção inválida: PUBLIC_RESTAURANT_URL_PREFIX não é uma URL válida.");
        }
        String host = publicUri.getHost() == null ? "" : publicUri.getHost().toLowerCase(Locale.ROOT);
        if (!"https".equalsIgnoreCase(publicUri.getScheme())
                || host.isBlank()
                || "localhost".equals(host)
                || "127.0.0.1".equals(host)) {
            throw new IllegalStateException(
                    "Configuração de produção inválida: PUBLIC_RESTAURANT_URL_PREFIX deve utilizar HTTPS em domínio de produção.");
        }
    }

    private static void validateIndependentProductionSecret(
            String secret,
            String secretName,
            String otherSecret,
            String otherSecretName
    ) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Configuração de produção inválida: " + secretName + " é obrigatório.");
        }
        String trimmed = secret.trim();
        if (trimmed.equals(otherSecret.trim())) {
            throw new IllegalStateException(
                    "Configuração de produção inválida: " + secretName
                            + " deve ser independente de " + otherSecretName
                            + " (fallback compartilhado não permitido em produção).");
        }
        if (trimmed.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES
                || INSECURE_SECRETS.contains(trimmed.toLowerCase(Locale.ROOT))) {
            throw new IllegalStateException(
                    "Configuração de produção inválida: " + secretName
                            + " é fraco ou utiliza um valor padrão inseguro (mínimo 32 bytes).");
        }
    }
}
