package com.borae.assistant;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1/public/restaurants/{slug}/assistant")
@Tag(name = "Assistente gastronômico", description = "Recomendações anônimas baseadas no cardápio público")
public class PublicAssistantInteractionController {

    private final GastronomicAssistantService assistantService;
    private final AssistantIpRateLimitService ipRateLimitService;
    private final AssistantRecommendationClickService clickService;

    public PublicAssistantInteractionController(
            GastronomicAssistantService assistantService,
            AssistantIpRateLimitService ipRateLimitService,
            AssistantRecommendationClickService clickService
    ) {
        this.assistantService = assistantService;
        this.ipRateLimitService = ipRateLimitService;
        this.clickService = clickService;
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
            @Valid @RequestBody AssistantInteractionRequest request,
            HttpServletRequest httpRequest
    ) {
        ipRateLimitService.checkAndConsume(httpRequest);
        return assistantService.interact(slug, sessionCookie, request);
    }
    @PostMapping("/recommendations/{productId}/click")
    @Operation(
            summary = "Registrar clique em recomendação",
            description = "Registra somente um contador agregado para um produto disponível no cardápio público. Requer sessão anônima válida."
    )
    public ResponseEntity<Void> recordRecommendationClick(
            @PathVariable String slug,
            @PathVariable java.util.UUID productId,
            @CookieValue(name = AnonymousAssistantSessionService.COOKIE_NAME, required = false) String sessionCookie,
            HttpServletRequest httpRequest
    ) {
        ipRateLimitService.checkAndConsume(httpRequest);
        clickService.recordClick(slug, sessionCookie, productId);
        return ResponseEntity.noContent().build();
    }

}
