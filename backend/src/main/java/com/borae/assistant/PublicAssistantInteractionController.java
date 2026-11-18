package com.borae.assistant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/restaurants/{slug}/assistant")
@Tag(name = "Assistente gastronômico", description = "Recomendações anônimas baseadas no cardápio público")
public class PublicAssistantInteractionController {

    private final GastronomicAssistantService assistantService;

    public PublicAssistantInteractionController(GastronomicAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping(value = "/interactions", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Solicitar recomendações gastronômicas",
            description = "Usa somente produtos disponíveis do cardápio publicado identificado pelo slug. Requer cookie de sessão emitido pelo endpoint de bootstrap."
    )
    public AssistantInteractionResponse interact(
            @PathVariable String slug,
            @CookieValue(name = AnonymousAssistantSessionService.COOKIE_NAME, required = false) String sessionCookie,
            @Valid @RequestBody AssistantInteractionRequest request
    ) {
        return assistantService.interact(slug, sessionCookie, request);
    }
}
