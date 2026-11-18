package com.borae.assistant;

import com.borae.catalog.ProductAvailability;
import com.borae.catalog.PublicRestaurantMenuResponse;
import com.borae.catalog.PublicRestaurantMenuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class GastronomicAssistantServiceTest {

    private static final UUID RESTAURANT_ID = UUID.randomUUID();
    private static final UUID MENU_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID FOREIGN_PRODUCT_ID = UUID.randomUUID();
    private static final UUID UNAVAILABLE_PRODUCT_ID = UUID.randomUUID();

    @Mock private PublicRestaurantMenuService publicMenuService;
    @Mock private AnonymousAssistantSessionService sessionService;
    @Mock private AssistantQuotaService quotaService;
    @Mock private GeminiFlashLiteProvider aiProvider;

    private GastronomicAssistantService service;

    @BeforeEach
    void setUp() {
        service = new GastronomicAssistantService(
                publicMenuService, sessionService, quotaService, aiProvider,
                new com.fasterxml.jackson.databind.ObjectMapper()
        );
        lenient().when(sessionService.quotaKey("valid-cookie")).thenReturn("opaque-hmac-key");
        lenient().when(aiProvider.isAvailable()).thenReturn(true);
        lenient().when(publicMenuService.getBySlug("restaurante-demo")).thenReturn(publicMenu());
        lenient().when(quotaService.consume(RESTAURANT_ID, "opaque-hmac-key")).thenReturn(
                new AssistantQuotaService.QuotaConsumption(
                        19, 99, ZonedDateTime.now(ZoneId.of("America/Sao_Paulo")).plusDays(1)
                )
        );
    }

    @Test
    void rejectsMissingOrInvalidAnonymousSessionBeforeCallingProvider() {
        when(sessionService.quotaKey(null)).thenReturn(null);

        assertThrows(ResponseStatusException.class,
                () -> service.interact("restaurante-demo", null,
                        new AssistantInteractionRequest("Quero uma massa", null, null)));

        verifyNoInteractions(aiProvider, quotaService);
    }

    @Test
    void returnsOnlyUniqueProductsFromThePublishedAvailableCatalog() {
        when(aiProvider.recommend(any(), any())).thenReturn(new GeminiFlashLiteProvider.ModelAnswer(
                "Separei uma opção para você.",
                List.of(
                        new GeminiFlashLiteProvider.ModelRecommendation(PRODUCT_ID, "Combina com sua preferência."),
                        new GeminiFlashLiteProvider.ModelRecommendation(PRODUCT_ID, "Duplicado."),
                        new GeminiFlashLiteProvider.ModelRecommendation(UNAVAILABLE_PRODUCT_ID, "Produto indisponível."),
                        new GeminiFlashLiteProvider.ModelRecommendation(FOREIGN_PRODUCT_ID, "Não pertence ao catálogo.")
                )
        ));

        AssistantInteractionResponse response = service.interact(
                "restaurante-demo", "valid-cookie",
                new AssistantInteractionRequest("Quero uma massa", null, null)
        );

        assertFalse(response.fallback());
        assertEquals(1, response.recommendations().size());
        assertEquals(PRODUCT_ID, response.recommendations().getFirst().productId());
        assertEquals("Produto real", response.recommendations().getFirst().name());
        assertEquals(2590, response.recommendations().getFirst().priceInCents());
        verify(quotaService).consume(RESTAURANT_ID, "opaque-hmac-key");
    }

    @Test
    void returnsFallbackWhenGeminiIsUnavailable() {
        when(aiProvider.recommend(any(), any()))
                .thenThrow(new GeminiFlashLiteProvider.AssistantProviderUnavailableException());

        AssistantInteractionResponse response = service.interact(
                "restaurante-demo", "valid-cookie",
                new AssistantInteractionRequest("Quero uma massa", null, null)
        );

        assertTrue(response.fallback());
        assertEquals("AI_UNAVAILABLE", response.fallbackCode());
        assertTrue(response.recommendations().isEmpty());
        assertNotNull(response.quota());
    }

    private PublicRestaurantMenuResponse publicMenu() {
        var product = new PublicRestaurantMenuResponse.PublicProduct(
                PRODUCT_ID, "Produto real", "Descrição", 2590, ProductAvailability.AVAILABLE,
                0, List.of("tomate", "manjericão"), List.of(), null, null
        );
        var unavailable = new PublicRestaurantMenuResponse.PublicProduct(
                UNAVAILABLE_PRODUCT_ID, "Produto indisponível", null, 1990, ProductAvailability.UNAVAILABLE,
                1, List.of(), List.of(), null, null
        );
        var category = new PublicRestaurantMenuResponse.PublicCategory(
                CATEGORY_ID, "Massas", null, 0, List.of(product, unavailable)
        );
        var menu = new PublicRestaurantMenuResponse.PublicMenu(
                MENU_ID, "Principal", null, 0, List.of(category)
        );
        return new PublicRestaurantMenuResponse(
                RESTAURANT_ID, "Restaurante Demo", "restaurante-demo", null,
                null, null, null, null, "Italiana", List.of(menu)
        );
    }
}
