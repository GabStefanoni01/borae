package com.borae;

import com.borae.identity.RestaurantObjectStorage;
import com.borae.security.RateLimitService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Homologação ponta a ponta do fluxo principal do Boraê em PostgreSQL 16 real (Testcontainers no CI).
 *
 * <p>Cobre integralmente: cadastro e autenticação, perfil e horários do restaurante, mídia e isolamento
 * de imagens de produto, gestão de equipe e convites, isolamento multi-tenant, cardápios múltiplos,
 * categorias e produtos (incluindo casos de borda e atualização de ingredientes/adicionais em PostgreSQL),
 * validação D-01 na publicação, reflexo imediato D-03, página pública sem JWT e decodificação real do
 * QR Code PNG via ZXing.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CoreFlowPostgresIntegrationTest {

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
        registry.add("security.rate-limit.default-max", () -> "500");
        registry.add("security.rate-limit.admin-sensitive-max", () -> "500");
        registry.add("security.rate-limit.register-max", () -> "100");
        registry.add("security.rate-limit.peak-max", () -> "500");
    }

    @TestConfiguration
    static class StubObjectStorageConfig {
        @Bean
        @Primary
        RestaurantObjectStorage stubRestaurantObjectStorage() {
            return new RestaurantObjectStorage() {
                @Override
                public StoredObject upload(String folder, String fileName, byte[] content, String contentType) {
                    String fileId = "stub-" + UUID.randomUUID();
                    return new StoredObject(fileId, "https://ik.imagekit.io/borae-test" + folder + "/" + fileName);
                }

                @Override
                public void delete(String fileId) {
                    // No-op em ambiente de teste de integração.
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RateLimitService rateLimitService;

    @BeforeEach
    void resetRateLimits() {
        rateLimitService.resetAll();
    }

    @Test
    void test01_authRegisterLoginSessionAndValidationErrors() throws Exception {
        String email = uniqueEmail("auth-owner");

        // Campos inválidos -> 400
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "A",
                                "email", "invalido",
                                "password", "123"
                        ))))
                .andExpect(status().isBadRequest());

        // Cadastro válido -> 201
        MvcResult registered = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Maria Silva",
                                "email", email,
                                "password", "SenhaForte@2026"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.email").value(email))
                .andReturn();

        // E-mail duplicado -> 409
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", "Maria Duplicada",
                                "email", email,
                                "password", "SenhaForte@2026"
                        ))))
                .andExpect(status().isConflict());

        // Login com senha incorreta -> 401
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", "SenhaErrada@999"
                        ))))
                .andExpect(status().isUnauthorized());

        // Login válido -> 200
        String token = loginAndGetToken(email, "SenhaForte@2026");

        // Recuperação de sessão e restaurantes vinculados
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Maria Silva"))
                .andExpect(jsonPath("$.email").value(email));

        mockMvc.perform(get("/api/v1/users/me/restaurants")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Acesso sem token -> 401
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized());

        assertNotNull(objectMapper.readTree(registered.getResponse().getContentAsString()).get("accessToken"));
    }

    @Test
    void test02_restaurantProfileHoursMediaIsolationAndD01Publication() throws Exception {
        String ownerToken = registerAndGetToken("Carlos Proprietário", uniqueEmail("profile-owner"));
        String restaurantId = createRestaurant(ownerToken, "Cantina do Centro", "Massas artesanais");

        // D-01: tentar publicar sem categoria gastronômica e endereço deve falhar com 400
        mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/publish")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("categoria")))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("endereço")));

        // Conferir no PostgreSQL que o restaurante permaneceu em DRAFT
        assertEquals("DRAFT", jdbcTemplate.queryForObject(
                "SELECT publication_status FROM restaurant WHERE id = ?",
                String.class, UUID.fromString(restaurantId)));

        // Coordenada incompleta (apenas latitude sem longitude) -> 400
        mockMvc.perform(patch("/api/v1/restaurants/" + restaurantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("latitude", -23.5505200))))
                .andExpect(status().isBadRequest());

        // Atualizar perfil completo incluindo campos obrigatórios de D-01
        String customSlug = "cantina-centro-" + UUID.randomUUID().toString().substring(0, 6);
        mockMvc.perform(patch("/api/v1/restaurants/" + restaurantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.ofEntries(
                                Map.entry("name", "Cantina do Centro SP"),
                                Map.entry("slug", customSlug),
                                Map.entry("description", "Culinária italiana artesanal no centro"),
                                Map.entry("category", "Italiana"),
                                Map.entry("address", "Rua Augusta, 500 - São Paulo/SP"),
                                Map.entry("phone", "(11) 3333-4444"),
                                Map.entry("whatsapp", "11999998888"),
                                Map.entry("instagram", "@cantinacentro"),
                                Map.entry("website", "https://cantinacentro.example.com"),
                                Map.entry("latitude", -23.5505200),
                                Map.entry("longitude", -46.6333080)
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cantina do Centro SP"))
                .andExpect(jsonPath("$.slug").value(customSlug))
                .andExpect(jsonPath("$.category").value("Italiana"))
                .andExpect(jsonPath("$.address").value("Rua Augusta, 500 - São Paulo/SP"));

        // Persistência real após recarregar
        mockMvc.perform(get("/api/v1/restaurants/" + restaurantId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Cantina do Centro SP"))
                .andExpect(jsonPath("$.category").value("Italiana"))
                .andExpect(jsonPath("$.address").value("Rua Augusta, 500 - São Paulo/SP"));

        // Horários de funcionamento com dois intervalos no mesmo dia
        mockMvc.perform(put("/api/v1/restaurants/" + restaurantId + "/business-hours")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "days", List.of(Map.of(
                                        "dayOfWeek", "MONDAY",
                                        "intervals", List.of(
                                                Map.of("opensAt", "18:00", "closesAt", "23:00"),
                                                Map.of("opensAt", "11:30", "closesAt", "15:00")
                                        )
                                ))
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].dayOfWeek").value("MONDAY"))
                .andExpect(jsonPath("$[0].intervals[0].opensAt").value("11:30:00"))
                .andExpect(jsonPath("$[0].intervals[1].opensAt").value("18:00:00"));

        // Intervalos sobrepostos -> 400
        mockMvc.perform(put("/api/v1/restaurants/" + restaurantId + "/business-hours")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "days", List.of(Map.of(
                                        "dayOfWeek", "TUESDAY",
                                        "intervals", List.of(
                                                Map.of("opensAt", "11:00", "closesAt", "15:00"),
                                                Map.of("opensAt", "14:00", "closesAt", "19:00")
                                        )
                                ))
                        ))))
                .andExpect(status().isBadRequest());

        // Upload de logo, foto e imagem de produto -> confirmar que GET /media não vaza PRODUCT_IMAGE
        MockMultipartFile validPng = createValidPngFile("logo.png");
        mockMvc.perform(multipart("/api/v1/restaurants/" + restaurantId + "/media")
                        .file(validPng)
                        .param("type", "LOGO")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("LOGO"));

        String menuId = createMenu(ownerToken, restaurantId, "Almoço", 0);
        String categoryId = createCategory(ownerToken, restaurantId, menuId, "Massas", 0);
        String productId = createProduct(ownerToken, restaurantId, menuId, categoryId, "Lasanha", "Molho bolonhesa", 4200L);

        mockMvc.perform(multipart("/api/v1/restaurants/" + restaurantId + "/menus/" + menuId + "/products/" + productId + "/image")
                        .file(createValidPngFile("lasanha.png"))
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrl").isNotEmpty());

        // GET /media deve retornar somente LOGO (1 item), sem PRODUCT_IMAGE
        mockMvc.perform(get("/api/v1/restaurants/" + restaurantId + "/media")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("LOGO"));

        // D-01 satisfeito: publicar restaurante agora funciona -> 200 PUBLISHED
        mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/publish")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"));

        // Enquanto PUBLISHED, limpar endereço ou categoria deve ser bloqueado (400)
        mockMvc.perform(patch("/api/v1/restaurants/" + restaurantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("address", ""))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void test03_teamInvitationsRolesRevocationAndMultiTenantIsolation() throws Exception {
        String ownerToken = registerAndGetToken("Ana Dona", uniqueEmail("team-owner"));
        String restaurantA = createRestaurant(ownerToken, "Restaurante Alpha", "Restaurante A");

        String managerEmail = uniqueEmail("team-manager");
        String managerToken = registerAndGetToken("Bruno Gerente", managerEmail);
        String staffEmail = uniqueEmail("team-staff");
        String staffToken = registerAndGetToken("Carla Equipe", staffEmail);
        String outsiderToken = registerAndGetToken("Diego Externo", uniqueEmail("team-outsider"));
        createRestaurant(outsiderToken, "Restaurante Beta", "Restaurante B");

        // OWNER não pode convidar papel OWNER -> 400
        mockMvc.perform(post("/api/v1/restaurants/" + restaurantA + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", managerEmail, "role", "OWNER"))))
                .andExpect(status().isBadRequest());

        // OWNER convida MANAGER e STAFF -> 201
        MvcResult managerInviteResult = mockMvc.perform(post("/api/v1/restaurants/" + restaurantA + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", managerEmail, "role", "MANAGER"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();
        String managerInviteToken = objectMapper.readTree(managerInviteResult.getResponse().getContentAsString())
                .get("token").asText();
        String managerInviteId = objectMapper.readTree(managerInviteResult.getResponse().getContentAsString())
                .get("invitationId").asText();

        // Confirmar que o banco não armazenou o token em texto puro
        String storedHash = jdbcTemplate.queryForObject(
                "SELECT token_hash FROM restaurant_invitation WHERE id = ?",
                String.class, UUID.fromString(managerInviteId));
        assertNotEquals(managerInviteToken, storedHash);
        assertEquals(64, storedHash.length());

        // Outro usuário tentando aceitar convite do gerente -> 403
        mockMvc.perform(post("/api/v1/restaurant-invitations/accept")
                        .header("Authorization", "Bearer " + outsiderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", managerInviteToken))))
                .andExpect(status().isForbidden());

        // Token inexistente -> 404
        mockMvc.perform(post("/api/v1/restaurant-invitations/accept")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", "token-invalido-xyz"))))
                .andExpect(status().isNotFound());

        // Gerente aceita convite -> 200
        mockMvc.perform(post("/api/v1/restaurant-invitations/accept")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", managerInviteToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));

        // Reutilização do mesmo token já aceito -> 410 Gone
        mockMvc.perform(post("/api/v1/restaurant-invitations/accept")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", managerInviteToken))))
                .andExpect(status().isGone());

        // Convite expirado -> 410 Gone
        String expiredEmail = uniqueEmail("team-expired");
        String expiredUserToken = registerAndGetToken("Eva Expirada", expiredEmail);
        MvcResult expiredInvite = mockMvc.perform(post("/api/v1/restaurants/" + restaurantA + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", expiredEmail, "role", "STAFF"))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode expiredJson = objectMapper.readTree(expiredInvite.getResponse().getContentAsString());
        jdbcTemplate.update(
                "UPDATE restaurant_invitation SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 day' WHERE id = ?",
                UUID.fromString(expiredJson.get("invitationId").asText()));

        mockMvc.perform(post("/api/v1/restaurant-invitations/accept")
                        .header("Authorization", "Bearer " + expiredUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", expiredJson.get("token").asText()))))
                .andExpect(status().isGone());

        // Convite e aceite de STAFF
        MvcResult staffInvite = mockMvc.perform(post("/api/v1/restaurants/" + restaurantA + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", staffEmail, "role", "STAFF"))))
                .andExpect(status().isCreated())
                .andReturn();
        String staffInviteToken = objectMapper.readTree(staffInvite.getResponse().getContentAsString())
                .get("token").asText();
        MvcResult staffAccepted = mockMvc.perform(post("/api/v1/restaurant-invitations/accept")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", staffInviteToken))))
                .andExpect(status().isOk())
                .andReturn();
        String staffMembershipId = objectMapper.readTree(staffAccepted.getResponse().getContentAsString())
                .get("membershipId").asText();

        // Permissões MANAGER vs STAFF
        mockMvc.perform(get("/api/v1/restaurants/" + restaurantA + "/members")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));

        mockMvc.perform(post("/api/v1/restaurants/" + restaurantA + "/invitations")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", uniqueEmail("blocked"), "role", "STAFF"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/restaurants/" + restaurantA + "/members")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/restaurants/" + restaurantA + "/qrcode")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());

        // Criar cardápio/produto como MANAGER e alterar disponibilidade como STAFF
        String menuId = createMenu(managerToken, restaurantA, "Menu Equipe", 0);
        String catId = createCategory(managerToken, restaurantA, menuId, "Bebidas", 0);
        String prodId = createProduct(managerToken, restaurantA, menuId, catId, "Suco Natural", null, 1200L);

        mockMvc.perform(patch("/api/v1/restaurants/" + restaurantA + "/menus/" + menuId + "/products/" + prodId + "/availability")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("availability", "UNAVAILABLE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").value("UNAVAILABLE"));

        // OWNER altera papel do STAFF -> MANAGER e depois remove o vínculo
        mockMvc.perform(patch("/api/v1/restaurants/" + restaurantA + "/members/" + staffMembershipId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "MANAGER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MANAGER"));

        mockMvc.perform(delete("/api/v1/restaurants/" + restaurantA + "/members/" + staffMembershipId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNoContent());

        // Após remoção, o usuário perde imediatamente acesso ao restaurante
        mockMvc.perform(get("/api/v1/restaurants/" + restaurantA + "/menus")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());

        // Isolamento multi-tenant: usuário externo (outsider) é bloqueado em todas as rotas de Restaurant A
        mockMvc.perform(get("/api/v1/restaurants/" + restaurantA)
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/restaurants/" + restaurantA + "/menus")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/restaurants/" + restaurantA + "/members")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/restaurants/" + restaurantA + "/qrcode")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/restaurants/" + restaurantA + "/publish")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void test04_fullCoreFlowCatalogEdgeCasesD03PublicPageAndRealQrCodeDecoding() throws Exception {
        String ownerToken = registerAndGetToken("Helena Chef", uniqueEmail("e2e-owner"));
        String restaurantId = createRestaurant(ownerToken, "Bistrô da Esquina", "Cozinha afetiva");

        String slug = "bistro-esquina-" + UUID.randomUUID().toString().substring(0, 6);
        mockMvc.perform(patch("/api/v1/restaurants/" + restaurantId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Bistrô da Esquina",
                                "slug", slug,
                                "category", "Contemporânea",
                                "address", "Av. Paulista, 1000 - São Paulo/SP",
                                "whatsapp", "11988887777",
                                "instagram", "@bistroesquina"
                        ))))
                .andExpect(status().isOk());

        // Criar 2 cardápios (Almoço e Jantar)
        String lunchMenuId = createMenu(ownerToken, restaurantId, "Almoço", 0);
        String dinnerMenuId = createMenu(ownerToken, restaurantId, "Jantar", 1);

        // Cardápio vazio não pode ser publicado -> 409
        mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/menus/" + lunchMenuId + "/publish")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isConflict());

        // Categorias: Entradas, Pratos Principais, Categoria Vazia + reordenação
        String catStarters = createCategory(ownerToken, restaurantId, lunchMenuId, "Entradas", 0);
        String catMains = createCategory(ownerToken, restaurantId, lunchMenuId, "Pratos Principais", 1);
        String catEmpty = createCategory(ownerToken, restaurantId, lunchMenuId, "Sobremesas", 2);

        // Nome duplicado ignorando acento/caixa -> 409
        mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/menus/" + lunchMenuId + "/categories")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "ENTRADÁS",
                                "displayOrder", 3
                        ))))
                .andExpect(status().isConflict());

        // Reordenar categorias (Pratos Principais primeiro)
        mockMvc.perform(put("/api/v1/restaurants/" + restaurantId + "/menus/" + lunchMenuId + "/categories/order")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(catMains, catStarters, catEmpty))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(catMains))
                .andExpect(jsonPath("$[1].id").value(catStarters));

        // Casos de borda de produtos: preço negativo (400) e categoria inexistente (404)
        mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/menus/" + lunchMenuId + "/products")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", catMains,
                                "name", "Preço Inválido",
                                "priceInCents", -50,
                                "displayOrder", 0,
                                "ingredients", List.of(),
                                "addons", List.of()
                        ))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/menus/" + lunchMenuId + "/products")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", UUID.randomUUID().toString(),
                                "name", "Categoria Inexistente",
                                "priceInCents", 1500,
                                "displayOrder", 0,
                                "ingredients", List.of(),
                                "addons", List.of()
                        ))))
                .andExpect(status().isNotFound());

        // Produto completo com ingredientes e adicionais (1 ativo + 1 inativo)
        MvcResult burgerCreated = mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/menus/" + lunchMenuId + "/products")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", catMains,
                                "name", "Hambúrguer Artesanal",
                                "description", "Blend 180g na brasa",
                                "priceInCents", 3290,
                                "displayOrder", 0,
                                "ingredients", List.of("Pão brioche", "Carne 180g", "Queijo prato"),
                                "addons", List.of(
                                        Map.of("name", "Bacon crocante", "priceDeltaInCents", 500, "active", true),
                                        Map.of("name", "Extra inativo", "priceDeltaInCents", 300, "active", false)
                                )
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String burgerId = objectMapper.readTree(burgerCreated.getResponse().getContentAsString()).get("id").asText();

        // Produto sem imagem e sem descrição (caso de borda) + marcado como UNAVAILABLE
        String waterId = createProduct(ownerToken, restaurantId, lunchMenuId, catStarters, "Água com Gás", null, 600L);
        mockMvc.perform(patch("/api/v1/restaurants/" + restaurantId + "/menus/" + lunchMenuId + "/products/" + waterId + "/availability")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("availability", "UNAVAILABLE"))))
                .andExpect(status().isOk());

        // Produto arquivado (não deve aparecer no cardápio público)
        String archivedId = createProduct(ownerToken, restaurantId, lunchMenuId, catStarters, "Item Arquivado", null, 990L);
        mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/menus/" + lunchMenuId + "/products/" + archivedId + "/archive")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lifecycleStatus").value("ARCHIVED"));

        // Antes da publicação do restaurante, página pública retorna 404
        mockMvc.perform(get("/api/v1/public/restaurants/" + slug + "/menu"))
                .andExpect(status().isNotFound());

        // Publicar cardápio Almoço (mantendo Jantar em DRAFT) e publicar restaurante
        mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/menus/" + lunchMenuId + "/publish")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/publish")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationStatus").value("PUBLISHED"));

        // QR Code real: baixar PNG e decodificar o conteúdo binário com ZXing
        MvcResult qrResult = mockMvc.perform(get("/api/v1/restaurants/" + restaurantId + "/qrcode")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andReturn();
        byte[] qrBytes = qrResult.getResponse().getContentAsByteArray();
        assertTrue(qrBytes.length > 100, "O QR Code PNG deve conter bytes válidos.");

        BufferedImage qrImage = ImageIO.read(new ByteArrayInputStream(qrBytes));
        assertNotNull(qrImage, "A resposta do endpoint /qrcode deve ser uma imagem PNG decodificável.");
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(qrImage)));
        Result decodedQr = new MultiFormatReader().decode(bitmap);
        String qrUrl = decodedQr.getText();
        assertEquals("http://localhost:4200/r/" + slug, qrUrl, "O QR Code deve codificar exatamente a URL pública do restaurante.");

        // Extrair o slug diretamente da URL decodificada do QR Code e consultar a página pública SEM JWT
        String decodedSlug = URI.create(qrUrl).getPath().replaceFirst("^/r/", "");
        MvcResult publicMenuResult = mockMvc.perform(get("/api/v1/public/restaurants/" + decodedSlug + "/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Bistrô da Esquina"))
                .andExpect(jsonPath("$.category").value("Contemporânea"))
                .andExpect(jsonPath("$.menus.length()").value(1))
                .andExpect(jsonPath("$.menus[0].name").value("Almoço"))
                .andExpect(jsonPath("$.menus[0].categories[0].name").value("Pratos Principais"))
                .andExpect(jsonPath("$.menus[0].categories[0].products[0].name").value("Hambúrguer Artesanal"))
                .andExpect(jsonPath("$.menus[0].categories[0].products[0].priceInCents").value(3290))
                .andExpect(jsonPath("$.menus[0].categories[0].products[0].ingredients.length()").value(3))
                .andExpect(jsonPath("$.menus[0].categories[0].products[0].addons.length()").value(1))
                .andExpect(jsonPath("$.menus[0].categories[0].products[0].addons[0].name").value("Bacon crocante"))
                .andExpect(jsonPath("$.menus[0].categories[1].name").value("Entradas"))
                .andExpect(jsonPath("$.menus[0].categories[1].products.length()").value(1))
                .andExpect(jsonPath("$.menus[0].categories[1].products[0].name").value("Água com Gás"))
                .andExpect(jsonPath("$.menus[0].categories[1].products[0].availability").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.menus[0].categories[2].products.length()").value(0))
                .andReturn();

        // Confirmar ausência de vazamento de dados internos/privados na resposta pública
        String publicJson = publicMenuResult.getResponse().getContentAsString();
        assertFalse(publicJson.contains("passwordHash"));
        assertFalse(publicJson.contains("membership"));
        assertFalse(publicJson.contains("Extra inativo"));
        assertFalse(publicJson.contains("Item Arquivado"));
        assertFalse(publicJson.contains(dinnerMenuId));

        // Consulta case-insensitive pelo slug na rota pública
        mockMvc.perform(get("/api/v1/public/restaurants/" + slug.toUpperCase(Locale.ROOT) + "/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value(slug));

        // Decisão D-03: editar produto em cardápio JÁ PUBLICADO (atualizando ingredientes/adicionais no PostgreSQL)
        // deve refletir imediatamente na página pública sem republicar
        mockMvc.perform(put("/api/v1/restaurants/" + restaurantId + "/menus/" + lunchMenuId + "/products/" + burgerId)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", catMains,
                                "name", "Burger Especial da Casa",
                                "description", "Blend 200g atualizado",
                                "priceInCents", 3590,
                                "displayOrder", 0,
                                "ingredients", List.of("Pão australiano", "Carne 200g"),
                                "addons", List.of(
                                        Map.of("name", "Cheddar cremoso", "priceDeltaInCents", 450, "active", true)
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Burger Especial da Casa"))
                .andExpect(jsonPath("$.priceInCents").value(3590));

        mockMvc.perform(get("/api/v1/public/restaurants/" + slug + "/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menus[0].categories[0].products[0].name").value("Burger Especial da Casa"))
                .andExpect(jsonPath("$.menus[0].categories[0].products[0].priceInCents").value(3590))
                .andExpect(jsonPath("$.menus[0].categories[0].products[0].ingredients[0]").value("Pão australiano"))
                .andExpect(jsonPath("$.menus[0].categories[0].products[0].addons[0].name").value("Cheddar cremoso"));

        // Desativar categoria que possui produtos -> some do cardápio público sem apagar produtos no banco
        mockMvc.perform(put("/api/v1/restaurants/" + restaurantId + "/menus/" + lunchMenuId + "/categories/" + catMains)
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Pratos Principais",
                                "displayOrder", 0,
                                "active", false
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(get("/api/v1/public/restaurants/" + slug + "/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menus[0].categories.length()").value(2))
                .andExpect(jsonPath("$.menus[0].categories[0].name").value("Entradas"));

        // Despublicar cardápio -> página pública retorna menus=[]
        mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/menus/" + lunchMenuId + "/unpublish")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(get("/api/v1/public/restaurants/" + slug + "/menu"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.menus.length()").value(0));

        // Despublicar restaurante -> página pública retorna 404 (mesma URL do QR Code)
        mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/unpublish")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.publicationStatus").value("DRAFT"));

        mockMvc.perform(get("/api/v1/public/restaurants/" + slug + "/menu"))
                .andExpect(status().isNotFound());

        // Slug inexistente -> 404
        mockMvc.perform(get("/api/v1/public/restaurants/restaurante-que-nao-existe-999/menu"))
                .andExpect(status().isNotFound());
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8) + "@borae-test.com";
    }

    private String registerAndGetToken(String fullName, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "fullName", fullName,
                                "email", email,
                                "password", "SenhaForte@2026"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", email,
                                "password", password
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    private String createRestaurant(String token, String name, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "description", description
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("restaurantId").asText();
    }

    private String createMenu(String token, String restaurantId, String name, int displayOrder) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/menus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "displayOrder", displayOrder
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createCategory(String token, String restaurantId, String menuId, String name, int displayOrder) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/menus/" + menuId + "/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name,
                                "displayOrder", displayOrder
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String createProduct(String token, String restaurantId, String menuId, String categoryId,
                                 String name, String description, long priceInCents) throws Exception {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("categoryId", categoryId);
        payload.put("name", name);
        payload.put("description", description);
        payload.put("priceInCents", priceInCents);
        payload.put("displayOrder", 0);
        payload.put("ingredients", List.of());
        payload.put("addons", List.of());

        MvcResult result = mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/menus/" + menuId + "/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private MockMultipartFile createValidPngFile(String fileName) throws Exception {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.ORANGE);
        graphics.fillRect(0, 0, 32, 32);
        graphics.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return new MockMultipartFile("file", fileName, "image/png", baos.toByteArray());
    }
}
