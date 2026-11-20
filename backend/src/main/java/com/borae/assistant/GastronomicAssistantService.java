package com.borae.assistant;

import com.borae.assistant.GeminiFlashLiteProvider.AssistantProviderUnavailableException;
import com.borae.assistant.GeminiFlashLiteProvider.ModelAnswer;
import com.borae.assistant.GeminiFlashLiteProvider.ModelRecommendation;
import com.borae.catalog.ProductAvailability;
import com.borae.catalog.PublicRestaurantMenuResponse;
import com.borae.catalog.PublicRestaurantMenuService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class GastronomicAssistantService {

    private static final int MAX_CATALOG_PRODUCTS = 80;
    private static final int MAX_CATALOG_CONTEXT_CHARS = 4_000;
    private static final int MAX_ANSWER_LENGTH = 420;
    private static final int MAX_REASON_LENGTH = 180;

    private static final String ALLERGY_CAUTION =
            "Para alergias ou restrições alimentares, confirme os ingredientes e o risco de contaminação cruzada diretamente com o restaurante.";

    private static final Pattern ALLERGY_OR_DIETARY_TRIGGER = Pattern.compile(
            "(?i)\\b(alerg\\w*|intoler\\w*|cel[ií]ac\\w*|gl[uú]ten|lactose|amendoim|castanh\\w*|nozes|"
                    + "marisco\\w*|camar[aã]o|frutos do mar|soja|leite|ovos?|diab[eé]t\\w*|restri[çc][aã]o|sem\\s+\\w+)\\b"
    );

    private static final Pattern UNSAFE_ALLERGEN_GUARANTEE = Pattern.compile(
            "(?i)(100%\\s*(livre|seguro|isento)|totalmente\\s+(livre|seguro|isento)|garant\\w*\\s+(que\\s+)?(n[aã]o\\s+cont[eé]m|livre|seguro)|"
                    + "zero\\s+contamina[çc][aã]o|sem\\s+risco\\s+de\\s+contamina[çc][aã]o|livre\\s+de\\s+al[eé]rgen\\w*|"
                    + "seguro\\s+para\\s+al[eé]rgic\\w*|completamente\\s+sem\\s+(gl[uú]ten|lactose|al[eé]rgen\\w*))"
    );

    private static final Pattern CURRENCY_PATTERN = Pattern.compile(
            "(?i)(r\\$\\s*\\d|\\d+[.,]\\d{2}\\s*(reais|brl))"
    );

    private static final Pattern PROMPT_LEAK_PATTERN = Pattern.compile(
            "(?i)(system\\s*prompt|instru[çc][oõ]es\\s*internas|regras\\s*do\\s*sistema|api[_\\s-]*key|x-goog-api-key|gemini_api_key)"
    );

    private static final Pattern CONFIRM_WITH_RESTAURANT_PATTERN = Pattern.compile(
            "(?i)(confirm\\w*.*restaurante|restaurante.*confirm\\w*|verifique\\s+com\\s+o\\s+(restaurante|estabelecimento)|contamina[çc][aã]o\\s+cruzada)"
    );

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
            String emptyAnswer = appendAllergyCautionIfNeeded(
                    "Não encontrei opções disponíveis com esses filtros no cardápio. Tente ajustar o orçamento, a categoria ou os ingredientes excluídos.",
                    request
            );
            return new AssistantInteractionResponse(emptyAnswer, List.of(), null, false, null);
        }

        List<CatalogProduct> boundedCatalog = eligible.stream().limit(MAX_CATALOG_PRODUCTS).toList();
        String catalogJson = serializeCatalog(menu, boundedCatalog);
        while (catalogJson.length() > MAX_CATALOG_CONTEXT_CHARS && !boundedCatalog.isEmpty()) {
            boundedCatalog = boundedCatalog.subList(0, boundedCatalog.size() - 1);
            catalogJson = serializeCatalog(menu, boundedCatalog);
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
            Set<UUID> seen = new HashSet<>();
            for (ModelRecommendation candidate : modelAnswer.recommendations()) {
                CatalogProduct product = allowed.get(candidate.productId());
                if (product == null || !seen.add(candidate.productId())) {
                    continue;
                }
                recommendations.add(new AssistantInteractionResponse.Recommendation(
                        product.productId(), product.name(), product.priceInCents(),
                        ProductAvailability.AVAILABLE, product.imageUrl(), product.ingredients(),
                        safeReason(candidate.reason(), product)
                ));
                if (recommendations.size() == 4) {
                    break;
                }
            }

            String answer = safeAnswer(modelAnswer.answer(), recommendations, menu.name(), request);
            metricsService.recordInteractionOutcome(
                    menu.id(), elapsedMillis(startedAt), false, recommendations.isEmpty(),
                    recommendations.stream().map(AssistantInteractionResponse.Recommendation::productId).toList(),
                    modelAnswer.inputTokens(), modelAnswer.outputTokens()
            );
            return new AssistantInteractionResponse(answer, List.copyOf(recommendations), quotaDto, false, null);
        } catch (AssistantProviderUnavailableException exception) {
            metricsService.recordProviderError(menu.id());
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
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank()).toList();

        return menu.menus().stream()
                .filter(item -> request.menuId() == null || item.id().equals(request.menuId()))
                .flatMap(item -> item.categories().stream()
                        .filter(category -> categoryIds.isEmpty() || categoryIds.contains(category.id()))
                        .flatMap(category -> category.products().stream()
                                .filter(product -> product.availability() == ProductAvailability.AVAILABLE)
                                .filter(product -> filters == null || filters.maxPriceInCents() == null
                                        || product.priceInCents() <= filters.maxPriceInCents())
                                .filter(product -> excluded.isEmpty() || product.ingredients().stream()
                                        .map(value -> value.toLowerCase(Locale.ROOT))
                                        .noneMatch(ingredient -> excluded.stream().anyMatch(ingredient::contains)))
                                .map(product -> new CatalogProduct(
                                        product.id(),
                                        product.name(),
                                        truncate(product.description(), 140),
                                        product.priceInCents(),
                                        item.name(),
                                        category.name(),
                                        product.ingredients(),
                                        product.addons() == null ? List.of() : product.addons().stream()
                                                .map(PublicRestaurantMenuResponse.PublicAddon::name)
                                                .toList(),
                                        product.imageUrl()
                                ))))
                .toList();
    }

    private String serializeCatalog(PublicRestaurantMenuResponse menu, List<CatalogProduct> products) {
        try {
            CatalogContext context = new CatalogContext(
                    menu.name(),
                    menu.category(),
                    products.stream().map(CatalogProductForModel::from).toList()
            );
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível preparar o catálogo do assistente.", exception);
        }
    }

    private long elapsedMillis(long startedAt) {
        return Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
    }

    private String safeAnswer(
            String rawAnswer,
            List<AssistantInteractionResponse.Recommendation> recommendations,
            String restaurantName,
            AssistantInteractionRequest request
    ) {
        if (recommendations.isEmpty()) {
            String fallbackEmpty = "Não encontrei opções disponíveis no cardápio de " + restaurantName
                    + " que atendam exatamente ao que você pediu. Tente ajustar suas preferências ou filtros.";
            return appendAllergyCautionIfNeeded(fallbackEmpty, request);
        }

        String sanitized = normalizeText(rawAnswer, MAX_ANSWER_LENGTH);
        if (sanitized == null || containsUnsafeClaim(sanitized)) {
            sanitized = "Separei algumas opções disponíveis no cardápio de " + restaurantName
                    + " que combinam com o que você procura.";
        }
        return appendAllergyCautionIfNeeded(sanitized, request);
    }

    private String safeReason(String rawReason, CatalogProduct product) {
        String sanitized = normalizeText(rawReason, MAX_REASON_LENGTH);
        if (sanitized == null || containsUnsafeClaim(sanitized)) {
            return "Opção disponível em " + product.category() + " no cardápio publicado.";
        }
        return sanitized;
    }

    private String appendAllergyCautionIfNeeded(String text, AssistantInteractionRequest request) {
        boolean hasExcludedIngredients = request.filters() != null
                && request.filters().excludedIngredients() != null
                && !request.filters().excludedIngredients().isEmpty();
        boolean mentionsDietaryOrAllergy = request.message() != null
                && ALLERGY_OR_DIETARY_TRIGGER.matcher(request.message()).find();

        if ((hasExcludedIngredients || mentionsDietaryOrAllergy)
                && !CONFIRM_WITH_RESTAURANT_PATTERN.matcher(text).find()) {
            return text + " " + ALLERGY_CAUTION;
        }
        return text;
    }

    private static boolean containsUnsafeClaim(String text) {
        return UNSAFE_ALLERGEN_GUARANTEE.matcher(text).find()
                || CURRENCY_PATTERN.matcher(text).find()
                || PROMPT_LEAK_PATTERN.matcher(text).find();
    }

    private static String normalizeText(String value, int maxLength) {
        if (value == null) return null;
        String cleaned = value
                .replaceAll("[\\p{Cntrl}&&[^\\s]]", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("[*_`#]+", "")
                .trim();
        if (cleaned.isBlank()) return null;
        if (cleaned.length() <= maxLength) return cleaned;
        return cleaned.substring(0, maxLength - 1).trim() + "…";
    }

    private static String truncate(String value, int maxLength) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) return trimmed;
        return trimmed.substring(0, maxLength - 1).trim() + "…";
    }

    private record CatalogContext(
            String restaurant,
            String category,
            List<CatalogProductForModel> products
    ) {}

    private record CatalogProductForModel(
            UUID productId,
            String name,
            String description,
            long priceInCents,
            String menu,
            String category,
            List<String> ingredients,
            List<String> addons
    ) {
        static CatalogProductForModel from(CatalogProduct product) {
            return new CatalogProductForModel(
                    product.productId(),
                    product.name(),
                    product.description(),
                    product.priceInCents(),
                    product.menuName(),
                    product.category(),
                    product.ingredients(),
                    product.addons()
            );
        }
    }

    private record CatalogProduct(
            UUID productId,
            String name,
            String description,
            long priceInCents,
            String menuName,
            String category,
            List<String> ingredients,
            List<String> addons,
            String imageUrl
    ) {}
}
