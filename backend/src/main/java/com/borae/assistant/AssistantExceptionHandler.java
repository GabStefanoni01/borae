package com.borae.assistant;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AssistantExceptionHandler {

    @ExceptionHandler(AssistantIpRateLimitService.AssistantIpRateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleIpRateLimit(
            AssistantIpRateLimitService.AssistantIpRateLimitExceededException exception
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "RATE_LIMIT_EXCEEDED");
        body.put("message", "Muitas solicitações em pouco tempo. Tente novamente em instantes.");
        body.put("retryAfterSeconds", exception.getRetryAfterSeconds());

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()))
                .body(body);
    }

    @ExceptionHandler(AssistantQuotaService.AssistantQuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuotaExceeded(
            AssistantQuotaService.AssistantQuotaExceededException exception
    ) {
        ZonedDateTime now = ZonedDateTime.now(AssistantQuotaService.BUSINESS_ZONE);
        ZonedDateTime resetsAt = LocalDate.now(AssistantQuotaService.BUSINESS_ZONE)
                .plusDays(1).atStartOfDay(AssistantQuotaService.BUSINESS_ZONE);
        long retryAfterSeconds = Math.max(1L, Duration.between(now, resetsAt).toSeconds());

        String message = exception.getScope() == AssistantQuotaService.QuotaScope.RESTAURANT
                ? "O assistente deste restaurante atingiu o limite diário de uso. Você ainda pode explorar o cardápio normalmente."
                : "Você atingiu o limite diário de perguntas ao assistente. O limite renova à meia-noite.";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", "DAILY_QUOTA_EXCEEDED");
        body.put("scope", exception.getScope().name());
        body.put("message", message);
        body.put("resetsAt", resetsAt);
        body.put("retryAfterSeconds", retryAfterSeconds);

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds))
                .body(body);
    }
}
