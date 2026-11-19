package com.borae.security;

import com.borae.catalog.MenuCategoryRepository;
import com.borae.catalog.MenuProductRepository;
import com.borae.catalog.MenuRepository;
import com.borae.catalog.Product3DModelAuditRepository;
import com.borae.catalog.Product3DModelRepository;
import com.borae.identity.RestaurantBusinessHoursRepository;
import com.borae.identity.RestaurantInvitationRepository;
import com.borae.identity.RestaurantMediaRepository;
import com.borae.identity.RestaurantMembershipRepository;
import com.borae.identity.RestaurantRepository;
import com.borae.identity.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private RateLimitService rateLimitService;
    @Autowired private SecurityAnomalyMonitor anomalyMonitor;
    @Autowired private SecurityAuditLogger auditLogger;

    @Autowired private Product3DModelAuditRepository modelAuditRepository;
    @Autowired private Product3DModelRepository modelRepository;
    @Autowired private MenuProductRepository productRepository;
    @Autowired private MenuCategoryRepository categoryRepository;
    @Autowired private MenuRepository menuRepository;
    @Autowired private RestaurantMediaRepository mediaRepository;
    @Autowired private RestaurantBusinessHoursRepository businessHoursRepository;
    @Autowired private RestaurantInvitationRepository invitationRepository;
    @Autowired private RestaurantMembershipRepository membershipRepository;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private UserAccountRepository userRepository;

    @Value("${security.jwt.secret}")
    private String configuredJwtSecret;

    @BeforeEach
    void resetState() {
        rateLimitService.resetAll();
        anomalyMonitor.reset();
        auditLogger.clearEvents();

        modelAuditRepository.deleteAll();
        modelRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        menuRepository.deleteAll();
        mediaRepository.deleteAll();
        businessHoursRepository.deleteAll();
        invitationRepository.deleteAll();
        membershipRepository.deleteAll();
        restaurantRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldRejectExpiredAndInvalidSignatureJwtsOnProtectedEndpoints() throws Exception {
        String email = "jwt-user-" + UUID.randomUUID() + "@example.com";
        registerAndGetToken("User JWT", email);

        SecretKey correctKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(configuredJwtSecret));
        Instant expiredTime = Instant.now().minusSeconds(300);
        String expiredToken = Jwts.builder()
                .subject(email)
                .issuer("borae-api")
                .issuedAt(Date.from(expiredTime.minusSeconds(900)))
                .expiration(Date.from(expiredTime))
                .signWith(correctKey, Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        SecretKey wrongKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode("QW5vdGhlclZhbGlkQjY0U2VjcmV0S2V5Rm9yQm9yYWVTZWN1cml0eVN1aXRlOTk="));
        String forgedToken = Jwts.builder()
                .subject(email)
                .issuer("borae-api")
                .issuedAt(new Date())
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(wrongKey, Jwts.SIG.HS256)
                .compact();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void shouldEnforceRoleBasedAccessControlForOwnerManagerAndStaff() throws Exception {
        String ownerToken = registerAndGetToken("Owner", "owner-" + UUID.randomUUID() + "@example.com");
        String restaurantId = createRestaurant(ownerToken, "Restaurante RBAC");

        // Convidar e aceitar STAFF
        String staffEmail = "staff-" + UUID.randomUUID() + "@example.com";
        String staffInviteToken = inviteMember(ownerToken, restaurantId, staffEmail, "STAFF");
        String staffToken = registerAndGetToken("Staff", staffEmail);
        String staffMembershipId = acceptInvitation(staffToken, staffInviteToken);

        // Convidar e aceitar MANAGER
        String managerEmail = "manager-" + UUID.randomUUID() + "@example.com";
        String managerInviteToken = inviteMember(ownerToken, restaurantId, managerEmail, "MANAGER");
        String managerToken = registerAndGetToken("Manager", managerEmail);
        acceptInvitation(managerToken, managerInviteToken);

        // OWNER/MANAGER criam cardápio, categoria e produto
        String menuId = createMenu(managerToken, restaurantId, "Cardápio Principal");
        String categoryId = createCategory(managerToken, restaurantId, menuId, "Pratos");
        String productId = createProduct(managerToken, restaurantId, menuId, categoryId, "Risoto", 4500L);

        // STAFF não pode acessar perfil administrativo, publicar restaurante, convidar, criar menu/categoria/produto ou gerar QR Code
        mockMvc.perform(get("/api/v1/restaurants/" + restaurantId)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/restaurants/" + restaurantId)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Tentativa Staff"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/publish")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/restaurants/" + restaurantId + "/qrcode")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/restaurants/" + restaurantId + "/members")
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/invitations")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "x@example.com", "role", "STAFF"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/menus")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Menu Staff", "displayOrder", 1))))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/restaurants/" + restaurantId + "/menus/" + menuId + "/products/" + productId)
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", categoryId,
                                "name", "Risoto Alterado",
                                "priceInCents", 100L,
                                "displayOrder", 0,
                                "ingredients", List.of("Arroz"),
                                "addons", List.of()
                        ))))
                .andExpect(status().isForbidden());

        // STAFF pode alterar exclusivamente a disponibilidade operacional
        mockMvc.perform(patch("/api/v1/restaurants/" + restaurantId + "/menus/" + menuId + "/products/" + productId + "/availability")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("availability", "UNAVAILABLE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.availability").value("UNAVAILABLE"));

        // MANAGER não pode criar convites nem alterar papel ou remover membros
        mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/invitations")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "novo@example.com", "role", "STAFF"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/restaurants/" + restaurantId + "/members/" + staffMembershipId)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("role", "MANAGER"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/restaurants/" + restaurantId + "/members/" + staffMembershipId)
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldEnforceStrictMultiTenantIsolationBetweenRestaurants() throws Exception {
        String ownerAToken = registerAndGetToken("Owner A", "ownera-" + UUID.randomUUID() + "@example.com");
        String restaurantAId = createRestaurant(ownerAToken, "Restaurante A");
        String menuAId = createMenu(ownerAToken, restaurantAId, "Menu A");
        String categoryAId = createCategory(ownerAToken, restaurantAId, menuAId, "Categoria A");
        String productAId = createProduct(ownerAToken, restaurantAId, menuAId, categoryAId, "Produto A", 2000L);

        String ownerBToken = registerAndGetToken("Owner B", "ownerb-" + UUID.randomUUID() + "@example.com");
        String restaurantBId = createRestaurant(ownerBToken, "Restaurante B");
        String menuBId = createMenu(ownerBToken, restaurantBId, "Menu B");
        String categoryBId = createCategory(ownerBToken, restaurantBId, menuBId, "Categoria B");
        String productBId = createProduct(ownerBToken, restaurantBId, menuBId, categoryBId, "Produto B", 3000L);

        // Owner A não pode ler nem alterar qualquer recurso do Restaurante B
        mockMvc.perform(get("/api/v1/restaurants/" + restaurantBId)
                        .header("Authorization", "Bearer " + ownerAToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/restaurants/" + restaurantBId)
                        .header("Authorization", "Bearer " + ownerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Invasão"))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/restaurants/" + restaurantBId + "/publish")
                        .header("Authorization", "Bearer " + ownerAToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/restaurants/" + restaurantBId + "/members")
                        .header("Authorization", "Bearer " + ownerAToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/restaurants/" + restaurantBId + "/qrcode")
                        .header("Authorization", "Bearer " + ownerAToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/restaurants/" + restaurantBId + "/menus")
                        .header("Authorization", "Bearer " + ownerAToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/restaurants/" + restaurantBId + "/menus/" + menuBId + "/products")
                        .header("Authorization", "Bearer " + ownerAToken))
                .andExpect(status().isForbidden());

        // Tentativa de IDOR cruzado: Owner A usando restaurantAId na rota mas apontando para menuBId / categoryBId / productBId
        mockMvc.perform(get("/api/v1/restaurants/" + restaurantAId + "/menus/" + menuBId)
                        .header("Authorization", "Bearer " + ownerAToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/restaurants/" + restaurantAId + "/menus/" + menuAId + "/categories/" + categoryBId)
                        .header("Authorization", "Bearer " + ownerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "Hijack",
                                "displayOrder", 0,
                                "active", true
                        ))))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/restaurants/" + restaurantAId + "/menus/" + menuAId + "/products/" + productBId + "/availability")
                        .header("Authorization", "Bearer " + ownerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("availability", "UNAVAILABLE"))))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/restaurants/" + restaurantAId + "/menus/" + menuAId + "/products/" + productAId)
                        .header("Authorization", "Bearer " + ownerAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", categoryBId,
                                "name", "Produto A movido para Categoria B",
                                "priceInCents", 2000L,
                                "displayOrder", 0,
                                "ingredients", List.of("Sal"),
                                "addons", List.of()
                        ))))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldEnforceHttpRateLimitingAndWafProtection() throws Exception {
        String ip = "198.51.100.200";
        String loginBody = objectMapper.writeValueAsString(Map.of(
                "email", "ratelimit@example.com",
                "password", "wrong-password-123"
        ));

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header("X-Forwarded-For", ip)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(loginBody))
                    .andExpect(status().isUnauthorized())
                    .andExpect(header().exists("X-RateLimit-Limit"))
                    .andExpect(header().exists("X-RateLimit-Remaining"));
        }

        // 11ª tentativa excede o limite de login (10 por 15 min) e retorna 429 Too Many Requests
        mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.error").value("rate_limit"));

        // WAF bloqueia ferramenta de varredura (sqlmap) com 403
        mockMvc.perform(get("/api/v1/public/restaurants/borae/menu")
                        .header("X-Forwarded-For", "198.51.100.201")
                        .header("User-Agent", "sqlmap/1.8"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void shouldAllowAuthorizedCorsOriginAndRejectUnauthorizedCorsOrigin() throws Exception {
        // Origem autorizada (http://localhost:4200)
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "http://localhost:4200")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));

        // Origem não autorizada
        mockMvc.perform(options("/api/v1/auth/login")
                        .header("Origin", "https://malicious-site.example.com")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }

    @Test
    void shouldRejectInvalidAndOversizedUploadsViaHttpMultipartAndIncludeSecurityHeaders() throws Exception {
        String ownerToken = registerAndGetToken("Owner Upload", "upload-" + UUID.randomUUID() + "@example.com");
        String restaurantId = createRestaurant(ownerToken, "Restaurante Upload");

        // Arquivo com extensão .png mas conteúdo de texto inválido -> 400 Bad Request
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file", "fake.png", "image/png", "not-a-valid-png-binary-content".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/v1/restaurants/" + restaurantId + "/media")
                        .file(invalidFile)
                        .param("type", "LOGO")
                        .header("Authorization", "Bearer " + ownerToken)
                        .header("X-Forwarded-Proto", "https"))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("X-XSS-Protection", "0"))
                .andExpect(header().string("Referrer-Policy", "strict-origin-when-cross-origin"))
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().exists("Strict-Transport-Security"))
                .andExpect(jsonPath("$.message", not(containsString("Exception"))));

        // Upload de logo acima de 5 MB -> 413 Payload Too Large
        MockMultipartFile oversizedLogo = new MockMultipartFile(
                "file", "huge-logo.png", "image/png", new byte[5 * 1024 * 1024 + 32]);

        mockMvc.perform(multipart("/api/v1/restaurants/" + restaurantId + "/media")
                        .file(oversizedLogo)
                        .param("type", "LOGO")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.status").value(413));
    }

    private String registerAndGetToken(String fullName, String email) throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "fullName", fullName,
                "email", email,
                "password", "senha-segura-123"
        ));
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .header("X-Forwarded-For", "10.0." + (Math.abs(email.hashCode()) % 250) + "." + (Math.abs(fullName.hashCode()) % 250))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("accessToken").asText();
    }

    private String createRestaurant(String token, String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/restaurants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("restaurantId").asText();
    }

    private String inviteMember(String ownerToken, String restaurantId, String email, String role) throws Exception {
        String response = mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/invitations")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "role", role))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("token").asText();
    }

    private String acceptInvitation(String userToken, String invitationToken) throws Exception {
        String response = mockMvc.perform(post("/api/v1/restaurant-invitations/accept")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("token", invitationToken))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("membershipId").asText();
    }

    private String createMenu(String token, String restaurantId, String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/menus")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name, "displayOrder", 0))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String createCategory(String token, String restaurantId, String menuId, String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/menus/" + menuId + "/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name, "displayOrder", 0))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String createProduct(String token, String restaurantId, String menuId, String categoryId, String name, long priceInCents) throws Exception {
        String response = mockMvc.perform(post("/api/v1/restaurants/" + restaurantId + "/menus/" + menuId + "/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "categoryId", categoryId,
                                "name", name,
                                "priceInCents", priceInCents,
                                "displayOrder", 0,
                                "ingredients", List.of("Ingrediente 1"),
                                "addons", List.of()
                        ))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }
}
