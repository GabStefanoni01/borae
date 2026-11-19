package com.borae.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Filtro de proteção HTTP (Shield) do Boraê, adaptado de {@code src/shield.ts},
 * {@code src/cabecalhos.ts} e {@code src/csp.ts} do Neves Shield (Neves Scholar).
 *
 * <p>Aplica cabeçalhos HTTP de segurança, inspeção WAF na URL e no User-Agent,
 * rate limiting diferenciado por rota/usuário/IP e monitoramento de anomalias.</p>
 */
public class BoraeSecurityShieldFilter extends OncePerRequestFilter {

    public static final String DEFAULT_CSP = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data: blob: https://ik.imagekit.io",
            "font-src 'self' data:",
            "connect-src 'self' https://ik.imagekit.io",
            "media-src 'self' blob: https://ik.imagekit.io",
            "worker-src 'self' blob:",
            "object-src 'none'",
            "base-uri 'self'",
            "form-action 'self'",
            "frame-ancestors 'none'"
    );

    private static final String PRIVATE_NO_STORE = "private, no-store, max-age=0, must-revalidate";

    private final ClientIpResolver ipResolver;
    private final WafRequestInspector wafInspector;
    private final RateLimitService rateLimitService;
    private final SecurityAnomalyMonitor anomalyMonitor;
    private final SecurityAuditLogger auditLogger;
    private final ObjectMapper objectMapper;
    private final boolean hstsAlwaysEnabled;
    private final String hstsHeaderValue;

    public BoraeSecurityShieldFilter(
            ClientIpResolver ipResolver,
            WafRequestInspector wafInspector,
            RateLimitService rateLimitService,
            SecurityAnomalyMonitor anomalyMonitor,
            SecurityAuditLogger auditLogger,
            ObjectMapper objectMapper,
            boolean hstsAlwaysEnabled
    ) {
        this(ipResolver, wafInspector, rateLimitService, anomalyMonitor, auditLogger,
                objectMapper, hstsAlwaysEnabled, 31536000L, false);
    }

    public BoraeSecurityShieldFilter(
            ClientIpResolver ipResolver,
            WafRequestInspector wafInspector,
            RateLimitService rateLimitService,
            SecurityAnomalyMonitor anomalyMonitor,
            SecurityAuditLogger auditLogger,
            ObjectMapper objectMapper,
            boolean hstsAlwaysEnabled,
            long hstsMaxAgeSeconds,
            boolean hstsIncludeSubDomains
    ) {
        this.ipResolver = ipResolver;
        this.wafInspector = wafInspector;
        this.rateLimitService = rateLimitService;
        this.anomalyMonitor = anomalyMonitor;
        this.auditLogger = auditLogger;
        this.objectMapper = objectMapper;
        this.hstsAlwaysEnabled = hstsAlwaysEnabled;
        long safeMaxAge = Math.max(0L, hstsMaxAgeSeconds);
        this.hstsHeaderValue = "max-age=" + safeMaxAge + (hstsIncludeSubDomains ? "; includeSubDomains" : "");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String method = request.getMethod() == null ? "GET" : request.getMethod();
        String path = request.getRequestURI() == null ? "/" : request.getRequestURI();

        applySecurityHeaders(request, response, path);

        if (HttpMethod.OPTIONS.matches(method) || path.startsWith("/actuator/health")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = ipResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        List<String> automationSignals = SecurityAnomalyMonitor.detectAutomationSignals(request);

        // 1. Verificar se o IP já está bloqueado por ataque anterior
        RateLimitService.IpBlock existingBlock = rateLimitService.getActiveIpBlock(clientIp);
        if (existingBlock != null) {
            int retryAfter = Math.max(1, (int) Math.ceil((existingBlock.untilEpochMs() - System.currentTimeMillis()) / 1000.0));
            observeAnomaly(clientIp, method, path, true, automationSignals);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf(retryAfter));
            writeJsonError(response, 429, "rate_limit",
                    "Atividade suspeita detectada. Aguarde antes de tentar novamente.", retryAfter);
            return;
        }

        // 2. Verificar ferramenta de varredura no User-Agent
        WafRequestInspector.Threat agentThreat = wafInspector.inspectUserAgent(userAgent);
        if (agentThreat != null) {
            rateLimitService.blockIp(clientIp, agentThreat.detail());
            auditLogger.logSecurityEvent(
                    agentThreat.severity(),
                    agentThreat.type(),
                    method,
                    path,
                    clientIp,
                    true,
                    agentThreat.detail()
            );
            observeAnomaly(clientIp, method, path, true, automationSignals);
            writeJsonError(response, 403, "bloqueado", "Atividade suspeita detectada.", null);
            return;
        }

        // 3. Verificar padrões de ataque na URL e query string
        WafRequestInspector.Threat urlThreat = wafInspector.inspectUrl(path, request.getQueryString());
        if (urlThreat != null && urlThreat.isHighOrCritical()) {
            rateLimitService.blockIp(clientIp, "ataque na URL: " + urlThreat.type());
            auditLogger.logSecurityEvent(
                    urlThreat.severity(),
                    urlThreat.type(),
                    method,
                    path,
                    clientIp,
                    true,
                    urlThreat.detail()
            );
            observeAnomaly(clientIp, method, path, true, automationSignals);
            writeJsonError(response, 403, "bloqueado", "Atividade suspeita detectada.", null);
            return;
        }

        // 4. Rate Limiting por rota + sujeito (usuário autenticado ou IP)
        String authenticatedSubject = resolveAuthenticatedSubject();
        RateLimitService.RateLimitDecision decision =
                rateLimitService.checkRequest(clientIp, authenticatedSubject, method, path);

        if (decision.limit() > 0) {
            response.setHeader("X-RateLimit-Limit", String.valueOf(decision.limit()));
        }
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(decision.retryAfterSeconds()));

        if (!decision.allowed()) {
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            auditLogger.logSecurityEvent(
                    "MEDIA",
                    "rate_limit",
                    method,
                    path,
                    clientIp,
                    true,
                    "regra=" + decision.ruleName()
            );
            observeAnomaly(clientIp, method, path, true, automationSignals);
            writeJsonError(response, 429, "rate_limit", decision.message(), decision.retryAfterSeconds());
            return;
        }

        observeAnomaly(clientIp, method, path, false, automationSignals);
        filterChain.doFilter(request, response);
    }

    private void applySecurityHeaders(HttpServletRequest request, HttpServletResponse response, String path) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("X-XSS-Protection", "0");
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        response.setHeader("Permissions-Policy", "camera=(self), geolocation=(self), microphone=(), payment=(), usb=()");
        response.setHeader("X-Permitted-Cross-Domain-Policies", "none");
        response.setHeader("Content-Security-Policy", DEFAULT_CSP);

        if (!path.startsWith("/api/v1/public/")) {
            response.setHeader("Cache-Control", PRIVATE_NO_STORE);
        }

        boolean trustedForwardedHttps = ipResolver.isTrustedProxy(request.getRemoteAddr())
                && "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        boolean isHttps = request.isSecure() || trustedForwardedHttps || hstsAlwaysEnabled;
        if (isHttps) {
            response.setHeader("Strict-Transport-Security", hstsHeaderValue);
        }
    }

    private void observeAnomaly(String clientIp, String method, String path, boolean rejected, List<String> signals) {
        SecurityAnomalyMonitor.Anomaly anomaly = anomalyMonitor.observe(clientIp, path, rejected, signals);
        if (anomaly != null) {
            auditLogger.logSecurityEvent(
                    "ALTA",
                    "anomalia",
                    method,
                    path,
                    clientIp,
                    false,
                    anomaly.detail()
            );
        }
    }

    private String resolveAuthenticatedSubject() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equalsIgnoreCase(name)) {
            return null;
        }
        return name;
    }

    private void writeJsonError(
            HttpServletResponse response,
            int status,
            String errorCode,
            String message,
            Integer retryAfterSeconds
    ) throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", status);
        payload.put("error", errorCode);
        payload.put("message", message);
        if (retryAfterSeconds != null) {
            payload.put("retryAfterSeconds", retryAfterSeconds);
        }
        payload.put("timestamp", Instant.now().toString());
        response.getWriter().write(objectMapper.writeValueAsString(payload));
    }
}
