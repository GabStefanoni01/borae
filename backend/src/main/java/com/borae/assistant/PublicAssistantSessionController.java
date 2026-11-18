package com.borae.assistant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bootstraps an anonymous browser session before a consumer starts an assistant conversation.
 */
@RestController
@RequestMapping("/api/v1/public/assistant")
@Tag(name = "Assistente gastronômico", description = "Sessão anônima para uso do assistente no cardápio público")
public class PublicAssistantSessionController {

    private final AnonymousAssistantSessionService sessionService;

    public PublicAssistantSessionController(AnonymousAssistantSessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/sessions")
    @Operation(
            summary = "Iniciar sessão anônima do assistente",
            description = "Emite cookie HttpOnly assinado com validade de 24 horas. Nenhuma conta de consumidor é necessária."
    )
    public ResponseEntity<Void> createSession() {
        AnonymousAssistantSessionService.IssuedSession session = sessionService.issue();
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, session.cookie().toString())
                .build();
    }
}
