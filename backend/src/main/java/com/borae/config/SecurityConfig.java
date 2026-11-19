package com.borae.config;

import com.borae.security.BoraeSecurityShieldFilter;
import com.borae.security.ClientIpResolver;
import com.borae.security.JwtAuthenticationFilter;
import com.borae.security.JwtService;
import com.borae.security.RateLimitService;
import com.borae.security.SecurityAnomalyMonitor;
import com.borae.security.SecurityAuditLogger;
import com.borae.security.WafRequestInspector;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtService jwtService,
            CorsConfigurationSource corsConfigurationSource,
            ClientIpResolver ipResolver,
            WafRequestInspector wafInspector,
            RateLimitService rateLimitService,
            SecurityAnomalyMonitor anomalyMonitor,
            SecurityAuditLogger auditLogger,
            ObjectMapper objectMapper,
            @Value("${security.headers.hsts-enabled:false}") boolean hstsAlwaysEnabled,
            @Value("${security.headers.hsts-max-age-seconds:31536000}") long hstsMaxAgeSeconds,
            @Value("${security.headers.hsts-include-subdomains:false}") boolean hstsIncludeSubDomains
    ) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtService, auditLogger, ipResolver);
        BoraeSecurityShieldFilter shieldFilter = new BoraeSecurityShieldFilter(
                ipResolver,
                wafInspector,
                rateLimitService,
                anomalyMonitor,
                auditLogger,
                objectMapper,
                hstsAlwaysEnabled,
                hstsMaxAgeSeconds,
                hstsIncludeSubDomains
        );

        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers.disable())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) -> {
                            auditLogger.logSecurityEvent(
                                    "MEDIA",
                                    "tentativa_sem_login",
                                    request.getMethod(),
                                    request.getRequestURI(),
                                    ipResolver.resolve(request),
                                    true,
                                    "Acesso a endpoint protegido sem credencial JWT válida."
                            );
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(
                                    "{\"status\":401,\"error\":\"unauthorized\",\"message\":\"Autenticação obrigatória.\",\"timestamp\":\""
                                            + Instant.now() + "\"}"
                            );
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            auditLogger.logSecurityEvent(
                                    "MEDIA",
                                    "acesso_negado",
                                    request.getMethod(),
                                    request.getRequestURI(),
                                    ipResolver.resolve(request),
                                    true,
                                    "Permissão insuficiente para o recurso solicitado."
                            );
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.getWriter().write(
                                    "{\"status\":403,\"error\":\"forbidden\",\"message\":\"Acesso negado.\",\"timestamp\":\""
                                            + Instant.now() + "\"}"
                            );
                        }))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/public/**",
                                "/actuator/health",
                                "/actuator/health/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(shieldFilter, JwtAuthenticationFilter.class)
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable())
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:4200}") String allowedOrigins
    ) {
        List<String> origins = Arrays.stream((allowedOrigins == null ? "" : allowedOrigins).split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();

        if (origins.isEmpty()) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS deve conter ao menos uma origem explícita autorizada.");
        }
        for (String origin : origins) {
            if ("*".equals(origin) || origin.contains("*")) {
                throw new IllegalStateException("CORS_ALLOWED_ORIGINS não permite o uso de wildcard (*).");
            }
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With"
        ));
        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Content-Disposition",
                "Retry-After",
                "X-RateLimit-Limit",
                "X-RateLimit-Remaining",
                "X-RateLimit-Reset"
        ));
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
