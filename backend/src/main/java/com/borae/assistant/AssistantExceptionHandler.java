package com.borae.assistant;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;

@RestControllerAdvice
public class AssistantExceptionHandler {

    @ExceptionHandler(AssistantIpRateLimitService.AssistantIpRateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleIpRateLimit(
            AssistantIpRateLimitService.AssistantIpRateLimitExceededException exception
    ) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(exception.getRetryAfterSeconds()))
                .body(Map.of(
                        "code", "RATE_LIMIT_EXCEEDED",
                        "message", "Muitas solicitações em pouco tempo. Tente novamente em instantes.",
                        "retryAfterSeconds", exception.getRetryAfterSeconds()
                ));
    }

    @ExceptionHandler(AssistantQuotaService.AssistantQuotaExceededException.class)
    public ResponseEntity<Map<String, Object>> handleQuotaExceeded(
            AssistantQuotaService.AssistantQuotaExceededException exception
    ) {
        ZonedDateTime resetsAt = LocalDate.now(AssistantQuotaService.BUSINESS_ZONE)
                .plusDays(1).atStartOfDay(AssistantQuotaService.BUSINESS_ZONE);

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                "code", "DAILY_QUOTA_EXCEEDED",
                "scope", exception.getScope().name(),
                "message", "O limite diário do assistente foi atingido.",
                "resetsAt", resetsAt
        ));
    }
}
