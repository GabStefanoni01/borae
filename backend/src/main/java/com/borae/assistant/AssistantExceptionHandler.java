package com.borae.assistant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Map;

@RestControllerAdvice
public class AssistantExceptionHandler {

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
