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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssistantRecommendationClickServiceTest {

    private static final UUID RESTAURANT_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID UNAVAILABLE_PRODUCT_ID = UUID.randomUUID();

    @Mock private PublicRestaurantMenuService publicMenuService;
    @Mock private AnonymousAssistantSessionService sessionService;
    @Mock private AssistantMetricsService metricsService;

    private AssistantRecommendationClickService service;

    @BeforeEach
    void setUp() {
        service = new AssistantRecommendationClickService(publicMenuService, sessionService, metricsService);
        lenient().when(sessionService.quotaKey("valid-cookie")).thenReturn("opaque-session-key");
        lenient().when(publicMenuService.getBySlug("restaurante-demo")).thenReturn(publicMenu());
    }

    @Test
    void recordsClickForAvailableProductWithValidSession() {
        service.recordClick("restaurante-demo", "valid-cookie", PRODUCT_ID);

        verify(metricsService).recordProductClick(RESTAURANT_ID, PRODUCT_ID);
    }

    @Test
    void rejectsUnavailableProduct() {
        assertThrows(ResponseStatusException.class,
                () -> service.recordClick("restaurante-demo", "valid-cookie", UNAVAILABLE_PRODUCT_ID));

        verifyNoInteractions(metricsService);
    }

    @Test
    void rejectsInvalidSessionBeforeLoadingMenu() {
        when(sessionService.quotaKey(null)).thenReturn(null);

        assertThrows(ResponseStatusException.class,
                () -> service.recordClick("restaurante-demo", null, PRODUCT_ID));

        verifyNoInteractions(publicMenuService, metricsService);
    }

    private PublicRestaurantMenuResponse publicMenu() {
        var available = new PublicRestaurantMenuResponse.PublicProduct(
                PRODUCT_ID, "Produto disponível", null, 2590, ProductAvailability.AVAILABLE,
                0, List.of("tomate"), List.of(), null, null
        );
        var unavailable = new PublicRestaurantMenuResponse.PublicProduct(
                UNAVAILABLE_PRODUCT_ID, "Produto indisponível", null, 1990, ProductAvailability.UNAVAILABLE,
                1, List.of(), List.of(), null, null
        );
        var category = new PublicRestaurantMenuResponse.PublicCategory(
                UUID.randomUUID(), "Categoria", null, 0, List.of(available, unavailable)
        );
        var menu = new PublicRestaurantMenuResponse.PublicMenu(
                UUID.randomUUID(), "Principal", null, 0, List.of(category)
        );
        return new PublicRestaurantMenuResponse(
                RESTAURANT_ID, "Restaurante Demo", "restaurante-demo", null,
                null, null, null, null, "Italiana", List.of(menu)
        );
    }
}
