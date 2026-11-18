package com.borae.assistant;

import com.borae.assistant.GeminiFlashLiteProvider.AssistantProviderUnavailableException;
import com.borae.assistant.GeminiFlashLiteProvider.ModelAnswer;
import com.borae.assistant.GeminiFlashLiteProvider.ModelRecommendation;
import com.borae.catalog.PublicRestaurantMenuResponse;
import com.borae.catalog.PublicRestaurantMenuService;
import com.borae.catalog.ProductAvailability;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GastronomicAssistantService {

    private static final int MAX_CATALOG_PRODUCTS = 80;
    private static final int MAX_CATALOG_CONTEXT_CHARS = 4_000;

    private final PublicRestaurantMenuService publicMenuService;
    private final AnonymousAssistantSessionService sessionService;
    private final AssistantQuotaService quotaService;
    private final AssistantMetricsService metricsService;
    private final GeminiFlashLiteProvider aiProvider;
    private final ObjectMapper objectMapper;

    public GastronomicAssistantService(
            PublicRestaurantMenuService publicMenuService,
            AnonymousAssistantSessionService sessionService,
            AssistantQuotaService quotaService,
            AssistantMetricsService metricsService,
            GeminiFlashLiteProvider aiProvider,
            ObjectMapper objectMapper
    ) {
        this.publicMenuService = publicMenuService;
        this.sessionService = sessionService;
        this.quotaService = quotaService;
        this.metricsService = metricsService;
        this.aiProvider = aiProvider;
        this.objectMapper = objectMapper;
    }

    public AssistantInteractionResponse interact(String slug, String sessionCookie,
                                                   AssistantInteractionRequest request) {
        long startedAt = System.nanoTime();
        String quotaKey = sessionService.quotaKey(sessionCookie);
        if (quotaKey == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicie uma sessão do assistente.");
        }

        PublicRestaurantMenuResponse menu = publicMenuService.getBySlug(slug);
        List<CatalogProduct> eligible = eligibleProducts(menu, request);
        if (eligible.isEmpty()) {
            metricsService.recordInteractionOutcome(menu.id(), elapsedMillis(startedAt),
                    false, true, List.of(), 0, 0);
            return new AssistantInteractionResponse(
                    "Não encontrei opções disponíveis com esses filtros. Tente ajustar o orçamento ou a categoria.",
                    List.of(), null, false, null
            );
        }

        List<CatalogProduct> boundedCatalog = eligible.stream().limit(MAX_CATALOG_PRODUCTS).toList();
        String catalogJson = serializeCatalog(boundedCatalog);
        while (catalogJson.length() > MAX_CATALOG_CONTEXT_CHARS && !boundedCatalog.isEmpty()) {
            boundedCatalog = boundedCatalog.subList(0, boundedCatalog.size() - 1);
            catalogJson = serializeCatalog(boundedCatalog);
        }
        if (boundedCatalog.isEmpty()) {
            return new AssistantInteractionResponse(
                    "Não encontrei opções disponíveis com esses filtros. Tente ajustar o orçamento ou a categoria.",
                    List.of(), null, false, null
            );
        }

        if (!aiProvider.isAvailable()) {
            metricsService.recordInteractionOutcome(menu.id(), elapsedMillis(startedAt),
                    true, false, List.of(), 0, 0);
            return new AssistantInteractionResponse(
                    "O assistente está temporariamente indisponível. Você ainda pode explorar o cardápio normalmente.",
                    List.of(), null, true, "AI_UNAVAILABLE"
            );
        }

        // Commit quota before the provider call: provider errors still consume the attempted interaction.
        AssistantQuotaService.QuotaConsumption quota = quotaService.consume(menu.id(), quotaKey);
        AssistantInteractionResponse.Quota quotaDto = new AssistantInteractionResponse.Quota(
                quota.remainingConsumer(), quota.remainingRestaurant(), quota.resetsAt()
        );

        try {
            ModelAnswer modelAnswer = aiProvider.recommend(request.message(), catalogJson);
            Map<UUID, CatalogProduct> allowed = boundedCatalog.stream()
                    .collect(Collectors.toMap(CatalogProduct::productId, Function.identity()));
            List<AssistantInteractionResponse.Recommendation> recommendations = new ArrayList<>();
            Set<UUID> seen = new java.util.HashSet<>();
            for (ModelRecommendation candidate : modelAnswer.recommendations()) {
                CatalogProduct product = allowed.get(candidate.productId());
                if (product == null || !seen.add(candidate.productId())) continue;
                recommendations.add(new AssistantInteractionResponse.Recommendation(
                        product.productId(), product.name(), product.priceInCents(),
                        ProductAvailability.AVAILABLE, product.imageUrl(), product.ingredients(),
                        safeReason(candidate.reason())
                ));
                if (recommendations.size() == 4) break;
            }

            // Keep user-facing prose grounded in backend-verified catalog data; never trust free-form model claims.
            String answer = recommendations.isEmpty()
                    ? "Não encontrei uma opção que atenda aos filtros informados."
                    : "Encontrei algumas opções disponíveis no cardápio que combinam com o que você procura.";
            metricsService.recordInteractionOutcome(
                    menu.id(), elapsedMillis(startedAt), false, recommendations.isEmpty(),
                    recommendations.stream().map(AssistantInteractionResponse.Recommendation::productId).toList(),
                    modelAnswer.inputTokens(), modelAnswer.outputTokens()
            );
            return new AssistantInteractionResponse(answer, List.copyOf(recommendations), quotaDto, false, null);
        } catch (AssistantProviderUnavailableException exception) {
            metricsService.recordInteractionOutcome(menu.id(), elapsedMillis(startedAt),
                    true, false, List.of(), 0, 0);
            return new AssistantInteractionResponse(
                    "Não consegui consultar o assistente agora. Você ainda pode explorar o cardápio normalmente.",
                    List.of(), quotaDto, true, "AI_UNAVAILABLE"
            );
        }
    }

    private List<CatalogProduct> eligibleProducts(PublicRestaurantMenuResponse menu,
                                                   AssistantInteractionRequest request) {
        AssistantInteractionRequest.Filters filters = request.filters();
        if (request.menuId() != null
                && menu.menus().stream().noneMatch(item -> item.id().equals(request.menuId()))) {
            return List.of();
        }

        Set<UUID> categoryIds = filters == null || filters.categoryIds() == null
                ? Set.of() : new HashSet<>(filters.categoryIds());
        List<String> excluded = filters == null || filters.excludedIngredients() == null
                ? List.of() : filters.excludedIngredients().stream()
                .map(value -> value.trim().toLowerCase(java.util.Locale.ROOT))
                .filter(value -> !value.isBlank()).toList();

        return menu.menus().stream()
                .filter(item -> request.menuId() == null || item.id().equals(request.menuId()))
                .flatMap(item -> item.categories().stream())
                .filter(category -> categoryIds.isEmpty() || categoryIds.contains(category.id()))
                .flatMap(category -> category.products().stream()
                        .filter(product -> product.availability() == ProductAvailability.AVAILABLE)
                        .filter(product -> filters == null || filters.maxPriceInCents() == null
                                || product.priceInCents() <= filters.maxPriceInCents())
                        .filter(product -> excluded.isEmpty() || product.ingredients().stream()
                                .map(value -> value.toLowerCase(java.util.Locale.ROOT))
                                .noneMatch(ingredient -> excluded.stream().anyMatch(ingredient::contains)))
                        .map(product -> new CatalogProduct(product.id(), product.name(), product.description(),
                                product.priceInCents(), category.name(), product.ingredients(), product.imageUrl())))
                .toList();
    }

    private String serializeCatalog(List<CatalogProduct> products) {
        try {
            return objectMapper.writeValueAsString(products);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível preparar o catálogo do assistente.", exception);
        }
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private String safeReason(String reason) {
        // Avoid exposing unverifiable model-generated claims about ingredients, allergens or preparation.
        return "Opção disponível no cardápio e compatível com os filtros selecionados.";
    }

    private record CatalogProduct(UUID productId, String name, String description, long priceInCents,
                                  String category, List<String> ingredients, String imageUrl) {}
}
