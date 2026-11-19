package com.borae.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Validador de segurança das configurações de inicialização em produção.
 *
 * <p>Garante falha fechada (fail-closed) caso variáveis obrigatórias estejam ausentes,
 * incompletas ou utilizem valores padrão inseguros no perfil de produção.</p>
 */
@Component
public class ProductionSecurityConfigValidator {

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

    private final Environment environment;
    private final String datasourceUrl;
    private final String datasourceUsername;
    private final String datasourcePassword;
    private final String corsAllowedOrigins;
    private final String publicRestaurantUrlPrefix;
    private final String jwtSecret;

    public ProductionSecurityConfigValidator(
            Environment environment,
            @Value("${spring.datasource.url:}") String datasourceUrl,
            @Value("${spring.datasource.username:}") String datasourceUsername,
            @Value("${spring.datasource.password:}") String datasourcePassword,
            @Value("${app.cors.allowed-origins:}") String corsAllowedOrigins,
            @Value("${app.public-restaurant-url-prefix:}") String publicRestaurantUrlPrefix,
            @Value("${security.jwt.secret:}") String jwtSecret
    ) {
        this.environment = environment;
        this.datasourceUrl = datasourceUrl;
        this.datasourceUsername = datasourceUsername;
        this.datasourcePassword = datasourcePassword;
        this.corsAllowedOrigins = corsAllowedOrigins;
        this.publicRestaurantUrlPrefix = publicRestaurantUrlPrefix;
        this.jwtSecret = jwtSecret;
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
}
