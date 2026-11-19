package com.borae.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Pattern;

/**
 * Canal dedicado de auditoria e logs de segurança, adaptado de {@code src/registro.ts}
 * e {@code src/shield.ts} do Neves Shield (Neves Scholar).
 *
 * <p>Nunca registra senhas, tokens JWT, tokens de convite, cookies, chaves secretas
 * ou o corpo bruto das requisições.</p>
 */
@Component
public class SecurityAuditLogger {

    private static final Logger SECURITY_LOG = LoggerFactory.getLogger("com.borae.security.audit");
    private static final String LOG_PREFIX = "[BORAE SECURITY]";
    private static final int MAX_RECENT_EVENTS = 500;
    private static final Pattern SENSITIVE_PATTERN = Pattern.compile(
            "(?i)(bearer\\s+[a-z0-9._\\-~+/]+=*|eyJ[a-z0-9._\\-]+|"
                    + "(?:password|token|secret|api[_-]?key|x-goog-api-key|BORA_ASSISTANT_SESSION)\\s*[:=]\\s*[^,\\s\"';}]+)"
    );

    private final Deque<SecurityEvent> recentEvents = new ConcurrentLinkedDeque<>();

    public record SecurityEvent(
            Instant timestamp,
            String severity,
            String eventType,
            String method,
            String path,
            String clientIp,
            boolean blocked,
            String detail
    ) {
    }

    public void logSecurityEvent(
            String severity,
            String eventType,
            String method,
            String path,
            String clientIp,
            boolean blocked,
            String detail
    ) {
        String safeSeverity = sanitize(severity, 16).toUpperCase();
        String safeType = sanitize(eventType, 48);
        String safeMethod = sanitize(method, 16);
        String safePath = sanitize(path, 200);
        String safeIp = sanitize(clientIp, 64);
        String safeDetail = redactSensitive(sanitize(detail, 240));

        SecurityEvent event = new SecurityEvent(
                Instant.now(),
                safeSeverity,
                safeType,
                safeMethod,
                safePath,
                safeIp,
                blocked,
                safeDetail
        );
        recentEvents.addLast(event);
        while (recentEvents.size() > MAX_RECENT_EVENTS) {
            recentEvents.pollFirst();
        }

        SECURITY_LOG.warn("{} {} {} {} ip={} tipo={} bloqueado={} :: {}",
                LOG_PREFIX, safeSeverity, safeMethod, safePath, safeIp, safeType, blocked, safeDetail);
    }

    public void logInternalError(String method, String path, String clientIp, Throwable exception) {
        String safeMethod = sanitize(method, 16);
        String safePath = sanitize(path, 200);
        String safeIp = sanitize(clientIp, 64);
        String exceptionType = exception == null ? "UnknownException" : exception.getClass().getSimpleName();
        SECURITY_LOG.error("{} ERRO_INTERNO {} {} ip={} exception={}",
                LOG_PREFIX, safeMethod, safePath, safeIp, exceptionType);
    }

    public List<SecurityEvent> getRecentEvents() {
        return new ArrayList<>(recentEvents);
    }

    public void clearEvents() {
        recentEvents.clear();
    }

    public static String redactSensitive(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        return SENSITIVE_PATTERN.matcher(input).replaceAll("[REDACTED]");
    }

    private static String sanitize(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "n/a";
        }
        String cleaned = value.replace('\r', ' ').replace('\n', ' ').replace('\t', ' ').trim();
        if (cleaned.length() > maxLength) {
            return cleaned.substring(0, maxLength);
        }
        return cleaned;
    }
}
