package com.borae.assistant;

import com.borae.catalog.ProductAvailability;
import com.borae.catalog.PublicRestaurantMenuService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AssistantRecommendationClickService {

    private final PublicRestaurantMenuService publicMenuService;
    private final AnonymousAssistantSessionService sessionService;
    private final AssistantMetricsService metricsService;

    public AssistantRecommendationClickService(
            PublicRestaurantMenuService publicMenuService,
            AnonymousAssistantSessionService sessionService,
            AssistantMetricsService metricsService
    ) {
        this.publicMenuService = publicMenuService;
        this.sessionService = sessionService;
        this.metricsService = metricsService;
    }

    public void recordClick(String slug, String sessionCookie, UUID productId) {
        if (sessionService.quotaKey(sessionCookie) == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicie uma sessão do assistente.");
        }

        var menu = publicMenuService.getBySlug(slug);
        boolean eligibleProduct = menu.menus().stream()
                .flatMap(item -> item.categories().stream())
                .flatMap(category -> category.products().stream())
                .anyMatch(product -> product.id().equals(productId)
                        && product.availability() == ProductAvailability.AVAILABLE);

        if (!eligibleProduct) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado no cardápio público.");
        }

        metricsService.recordProductClick(menu.id(), productId);
    }
}
