package com.borae.assistant;

import com.borae.assistant.AiProviderFailure.AiProviderException;
import com.borae.assistant.AiProviderFailure.FailureType;
import com.borae.catalog.Menu;
import com.borae.catalog.MenuCategory;
import com.borae.catalog.MenuCategoryRepository;
import com.borae.catalog.MenuProduct;
import com.borae.catalog.MenuProductRepository;
import com.borae.catalog.MenuRepository;
import com.borae.catalog.ProductAvailability;
import com.borae.identity.Restaurant;
import com.borae.identity.RestaurantRepository;
import com.borae.security.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração ponta a ponta do Assistente Gastronômico com PostgreSQL 16 (Flyway V1..V13),
 * cadeia completa do Spring Security e orquestração multi-provider (Prioridade 1: Gemini → Prioridade 2: Grok).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicAssistantFlowPostgresIntegrationTest {

    private static PostgreSQLContainer<?> postgresContainer;

    @DynamicPropertySource
    static void configurePostgresProperties(DynamicPropertyRegistry registry) {
        String externalUrl = System.getProperty("test.postgres.url");
        if (externalUrl != null && !externalUrl.isBlank()) {
            registry.add("spring.datasource.url", () -> externalUrl);
            registry.add("spring.datasource.username", () -> System.getProperty("test.postgres.username", "postgres"));
            registry.add("spring.datasource.password", () -> System.getProperty("test.postgres.password", "postgres"));
        } else {
            if (postgresContainer == null) {
                postgresContainer = new PostgreSQLContainer<>("postgres:16-alpine");
                postgresContainer.start();
            }
            registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
            registry.add("spring.datasource.username", postgresContainer::getUsername);
            registry.add("spring.datasource.password", postgresContainer::getPassword);
        }
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("security.rate-limit.storage-mode", () -> "postgres");
        registry.add("app.cors.allowed-origins", () -> "http://localhost:4200");
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private MenuRepository menuRepository;
    @Autowired private MenuCategoryRepository categoryRepository;
    @Autowired private MenuProductRepository productRepository;
    @Autowired private RateLimitService rateLimitService;
    @Autowired private AiProviderOrchestrator orchestrator;

    @MockitoBean
    private GeminiFlashLiteProvider geminiProvider;

    @MockitoBean
    private GrokAiProvider grokProvider;

    private String restaurantSlug;
    private UUID restaurantId;
    private UUID availableProductId;
    private UUID unavailableProductId;
    private UUID foreignProductId;

    @BeforeEach
    void setUp() {
        rateLimitService.resetAll();
        orchestrator.resetCircuitBreakers();
        jdbcTemplate.update("DELETE FROM assistant_daily_quota");
        jdbcTemplate.update("DELETE FROM assistant_ip_rate_limit");
        jdbcTemplate.update("DELETE FROM assistant_daily_metrics");
        jdbcTemplate.update("DELETE FROM assistant_daily_product_metrics");

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        restaurantSlug = "cantina-assistente-" + suffix;

        Restaurant restaurant = new Restaurant("Cantina Assistente", restaurantSlug, "Massas artesanais");
        restaurant.publish();
        restaurant = restaurantRepository.save(restaurant);
        restaurantId = restaurant.getId();

        Menu menu = new Menu(restaurant, "Cardápio Principal", "Servido o dia todo", 0);
        menu.publish();
        menu = menuRepository.save(menu);

        MenuCategory category = new MenuCategory(menu, "Massas", "Feitas na casa", 0);
        category = categoryRepository.save(category);

        MenuProduct available = new MenuProduct(
                category, "Spaghetti ao Sugo", "Molho de tomate pelati", 4200L, 0,
                List.of("spaghetti", "tomate pelati", "manjericão"), List.of()
        );
        available = productRepository.save(available);
        availableProductId = available.getId();

        MenuProduct unavailable = new MenuProduct(
                category, "Ravioli Esgotado", "Sem estoque hoje", 5900L, 1,
                List.of("massa fresca"), List.of()
        );
        unavailable.changeAvailability(ProductAvailability.UNAVAILABLE);
        unavailable = productRepository.save(unavailable);
        unavailableProductId = unavailable.getId();

        Restaurant other = new Restaurant("Outro Bistrô", "outro-bistro-" + suffix, "Outro tenant");
        other.publish();
        other = restaurantRepository.save(other);
        Menu otherMenu = new Menu(other, "Jantar", null, 0);
        otherMenu.publish();
        otherMenu = menuRepository.save(otherMenu);
        MenuCategory otherCat = categoryRepository.save(new MenuCategory(otherMenu, "Pratos", null, 0));
        MenuProduct foreign = productRepository.save(new MenuProduct(
                otherCat, "Prato de Outro Restaurante", null, 7500L, 0, List.of(), List.of()
        ));
        foreignProductId = foreign.getId();

        when(geminiProvider.id()).thenReturn(AiProviderId.GEMINI);
        when(grokProvider.id()).thenReturn(AiProviderId.GROK);
        when(geminiProvider.isAvailable()).thenReturn(true);
        when(grokProvider.isAvailable()).thenReturn(true);
    }

    @Test
    void fullPublicAssistantFlowValidatesCatalogQuotasClickTrackingAndPrivacy() throws Exception {
        // 1. CORS preflight supports credentials for explicitly allowed origin
        mockMvc.perform(options("/api/v1/public/assistant/sessions")
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));

        // 2. Rejects interaction without session cookie
        mockMvc.perform(post("/api/v1/public/restaurants/{slug}/assistant/interactions", restaurantSlug)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "Quero uma massa"))))
                .andExpect(status().isUnauthorized());

        // 3. Session bootstrap emits HttpOnly cookie and no-store cache header
        MvcResult sessionResult = mockMvc.perform(post("/api/v1/public/assistant/sessions"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("BORA_ASSISTANT_SESSION=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andReturn();

        Cookie sessionCookie = extractSessionCookie(sessionResult);
        assertNotNull(sessionCookie);

        // 4. Subsequent bootstrap reuses valid cookie (no new Set-Cookie)
        mockMvc.perform(post("/api/v1/public/assistant/sessions").cookie(sessionCookie))
                .andExpect(status().isNoContent())
                .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE));

        // 5. Interaction returns only available product of this restaurant (rejects unavailable & cross-tenant)
        when(geminiProvider.recommend(any(AiProviderRequest.class))).thenReturn(
                AiProviderResponse.of(
                        AiProviderId.GEMINI,
                        "Recomendo o Spaghetti ao Sugo para uma refeição clássica.",
                        List.of(
                                new AiProviderResponse.RecommendationCandidate(availableProductId, "Massa com molho de tomate pelati e manjericão."),
                                new AiProviderResponse.RecommendationCandidate(unavailableProductId, "Não deve aparecer"),
                                new AiProviderResponse.RecommendationCandidate(foreignProductId, "Outro restaurante")
                        ),
                        240,
                        60
                )
        );

        String secretUserPrompt = "Quero algo clássico de tomate — segredo-privado-nao-persistir";
        mockMvc.perform(post("/api/v1/public/restaurants/{slug}/assistant/interactions", restaurantSlug)
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", secretUserPrompt))))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$.fallback").value(false))
                .andExpect(jsonPath("$.recommendations", hasSize(1)))
                .andExpect(jsonPath("$.recommendations[0].productId").value(availableProductId.toString()))
                .andExpect(jsonPath("$.recommendations[0].name").value("Spaghetti ao Sugo"))
                .andExpect(jsonPath("$.recommendations[0].priceInCents").value(4200))
                .andExpect(jsonPath("$.quota.remainingConsumer").value(19))
                .andExpect(jsonPath("$.quota.remainingRestaurant").value(99));

        verify(grokProvider, never()).recommend(any());

        // 6. Case-insensitive slug normalization from Session 02 works seamlessly with assistant click
        mockMvc.perform(post("/api/v1/public/restaurants/{slug}/assistant/recommendations/{productId}/click",
                        restaurantSlug.toUpperCase(), availableProductId).cookie(sessionCookie))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/public/restaurants/{slug}/assistant/recommendations/{productId}/click",
                        restaurantSlug, unavailableProductId).cookie(sessionCookie))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/public/restaurants/{slug}/assistant/recommendations/{productId}/click",
                        restaurantSlug, foreignProductId).cookie(sessionCookie))
                .andExpect(status().isNotFound());

        // 7. Verify aggregate metrics recorded and no prompt text stored in assistant tables
        Long clicks = jdbcTemplate.queryForObject(
                "SELECT click_count FROM assistant_daily_product_metrics WHERE restaurant_id = ? AND product_id = ?",
                Long.class, restaurantId, availableProductId
        );
        assertEquals(1L, clicks);

        Integer quotaRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM assistant_daily_quota WHERE scope_key LIKE '%segredo-privado%'",
                Integer.class
        );
        assertEquals(0, quotaRows);
    }

    @Test
    void fallsBackFromGeminiToGrokEndToEndWithFullCatalogValidationAndAggregateMetrics() throws Exception {
        Cookie sessionCookie = extractSessionCookie(mockMvc.perform(post("/api/v1/public/assistant/sessions"))
                .andExpect(status().isNoContent()).andReturn());

        when(geminiProvider.recommend(any(AiProviderRequest.class))).thenThrow(
                new AiProviderException(AiProviderId.GEMINI, FailureType.HTTP_5XX, 503, "Gemini fora do ar")
        );
        when(grokProvider.recommend(any(AiProviderRequest.class))).thenReturn(
                AiProviderResponse.of(
                        AiProviderId.GROK,
                        "O Spaghetti ao Sugo é uma ótima escolha artesanal.",
                        List.of(
                                new AiProviderResponse.RecommendationCandidate(availableProductId, "Feito com tomate pelati e manjericão."),
                                new AiProviderResponse.RecommendationCandidate(foreignProductId, "Outro restaurante")
                        ),
                        200,
                        50
                )
        );

        mockMvc.perform(post("/api/v1/public/restaurants/{slug}/assistant/interactions", restaurantSlug)
                        .cookie(sessionCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("message", "O que sugere de massa?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fallback").value(false))
                .andExpect(jsonPath("$.recommendations", hasSize(1)))
                .andExpect(jsonPath("$.recommendations[0].productId").value(availableProductId.toString()))
                .andExpect(jsonPath("$.recommendations[0].priceInCents").value(4200));

        Long grokSuccess = jdbcTemplate.queryForObject(
                "SELECT metric_value FROM assistant_daily_metrics WHERE restaurant_id = ? AND metric_key = 'PROVIDER_GROK_SUCCESS'",
                Long.class, restaurantId
        );
        Long fallbackCount = jdbcTemplate.queryForObject(
                "SELECT metric_value FROM assistant_daily_metrics WHERE restaurant_id = ? AND metric_key = 'PROVIDER_FALLBACK_TO_GROK'",
                Long.class, restaurantId
        );
        assertEquals(1L, grokSuccess);
        assertEquals(1L, fallbackCount);
    }

    @Test
    void returnsStructured429WhenConsumerDailyQuotaIsExceededWithoutBlockingOtherSessionsOnSameIp() throws Exception {
        when(geminiProvider.recommend(any(AiProviderRequest.class))).thenReturn(
                AiProviderResponse.of(
                        AiProviderId.GEMINI,
                        "Sugiro o Spaghetti ao Sugo.",
                        List.of(new AiProviderResponse.RecommendationCandidate(availableProductId, "Clássico da casa.")),
                        100, 30
                )
        );

        Cookie sessionA = extractSessionCookie(mockMvc.perform(post("/api/v1/public/assistant/sessions"))
                .andExpect(status().isNoContent()).andReturn());
        Cookie sessionB = extractSessionCookie(mockMvc.perform(post("/api/v1/public/assistant/sessions"))
                .andExpect(status().isNoContent()).andReturn());

        String body = objectMapper.writeValueAsString(Map.of("message", "O que recomenda?"));
        for (int i = 0; i < AssistantQuotaService.CONSUMER_DAILY_LIMIT; i++) {
            mockMvc.perform(post("/api/v1/public/restaurants/{slug}/assistant/interactions", restaurantSlug)
                            .cookie(sessionA)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        // 21st request for session A hits structured DAILY_QUOTA_EXCEEDED (not generic filter 429)
        mockMvc.perform(post("/api/v1/public/restaurants/{slug}/assistant/interactions", restaurantSlug)
                        .cookie(sessionA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists(HttpHeaders.RETRY_AFTER))
                .andExpect(jsonPath("$.code").value("DAILY_QUOTA_EXCEEDED"))
                .andExpect(jsonPath("$.scope").value("CONSUMER"))
                .andExpect(jsonPath("$.resetsAt").exists());

        // Another customer (session B) on the SAME restaurant Wi-Fi IP can still interact!
        mockMvc.perform(post("/api/v1/public/restaurants/{slug}/assistant/interactions", restaurantSlug)
                        .cookie(sessionB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quota.remainingConsumer").value(19))
                .andExpect(jsonPath("$.quota.remainingRestaurant").value(79));
    }

    private static Cookie extractSessionCookie(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        if (setCookie == null) return null;
        String pair = setCookie.split(";", 2)[0];
        String[] parts = pair.split("=", 2);
        return new Cookie(parts[0].trim(), parts[1].trim());
    }
}
