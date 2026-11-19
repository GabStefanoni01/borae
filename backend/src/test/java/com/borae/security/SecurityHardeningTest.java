package com.borae.security;

import com.borae.catalog.GlbFileValidator;
import com.borae.catalog.Menu;
import com.borae.catalog.MenuCategory;
import com.borae.catalog.MenuProduct;
import com.borae.catalog.MenuProductRepository;
import com.borae.catalog.MenuRepository;
import com.borae.catalog.Product3DModelAuditRepository;
import com.borae.catalog.Product3DModelRepository;
import com.borae.catalog.Product3DModelService;
import com.borae.config.ApiExceptionHandler;
import com.borae.config.ProductionSecurityConfigValidator;
import com.borae.config.SecurityConfig;
import com.borae.identity.Restaurant;
import com.borae.identity.RestaurantImageProcessor;
import com.borae.identity.RestaurantMediaRepository;
import com.borae.identity.RestaurantMediaService;
import com.borae.identity.RestaurantMediaType;
import com.borae.identity.RestaurantMembership;
import com.borae.identity.RestaurantMembershipRepository;
import com.borae.identity.RestaurantObjectStorage;
import com.borae.identity.RestaurantRepository;
import com.borae.identity.RestaurantRole;
import com.borae.identity.UserAccount;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityHardeningTest {

    private static final String VALID_SECRET =
            "VGVzdE9ubHlCNjRTZWNyZXRLZXlGb3JCb3JhZVNlY3VyaXR5U3VpdGUyMDI2IQ==";
    private static final String OTHER_VALID_SECRET =
            "QW5vdGhlclZhbGlkQjY0U2VjcmV0S2V5Rm9yQm9yYWVTZWN1cml0eVN1aXRlOTk=";

    // 1. JWT_SECRET ausente
    @Test
    void shouldFailWhenJwtSecretIsMissingOrBlank() {
        assertThrows(IllegalStateException.class, () -> new JwtService(null, 900_000L));
        assertThrows(IllegalStateException.class, () -> new JwtService("", 900_000L));
        assertThrows(IllegalStateException.class, () -> new JwtService("   ", 900_000L));
    }

    // 2. JWT_SECRET inválido (não-Base64, curto, padrão antigo conhecido ou baixa entropia)
    @Test
    void shouldFailWhenJwtSecretIsInvalidWeakOrKnownDefault() {
        // Não-Base64
        assertThrows(IllegalStateException.class, () -> new JwtService("%%%not-base64%%%", 900_000L));
        // Menos de 32 bytes após decodificação
        assertThrows(IllegalStateException.class, () -> new JwtService("YWJjZGVmZ2hpamtsbW5vcA==", 900_000L));
        // Segredo padrão antigo conhecido ("0123456789abcdef0123456789abcdef")
        assertThrows(IllegalStateException.class,
                () -> new JwtService("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=", 900_000L));
        // Baixa entropia (32 bytes repetidos 'A')
        assertThrows(IllegalStateException.class,
                () -> new JwtService("QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUE=", 900_000L));
    }

    // 3. JWT expirado
    @Test
    void shouldRejectExpiredJwtToken() {
        JwtService jwtService = new JwtService(VALID_SECRET, 900_000L, "borae-api");
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(VALID_SECRET));
        Instant past = Instant.now().minusSeconds(600);
        String expiredToken = Jwts.builder()
                .subject("user@example.com")
                .issuer("borae-api")
                .issuedAt(Date.from(past.minusSeconds(900)))
                .expiration(Date.from(past))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        assertThrows(JwtException.class, () -> jwtService.extractSubject(expiredToken));
    }

    // 4. JWT com assinatura inválida, malformado ou com emissor inválido
    @Test
    void shouldRejectJwtWithInvalidSignatureOrIssuerOrMissingClaims() {
        JwtService jwtService = new JwtService(VALID_SECRET, 900_000L, "borae-api");
        JwtService attackerJwtService = new JwtService(OTHER_VALID_SECRET, 900_000L, "borae-api");

        String forgedToken = attackerJwtService.generateToken("admin@example.com");
        assertThrows(JwtException.class, () -> jwtService.extractSubject(forgedToken));

        JwtService wrongIssuerService = new JwtService(VALID_SECRET, 900_000L, "untrusted-issuer");
        String wrongIssuerToken = wrongIssuerService.generateToken("user@example.com");
        assertThrows(JwtException.class, () -> jwtService.extractSubject(wrongIssuerToken));

        assertThrows(JwtException.class, () -> jwtService.extractSubject("not.a.jwt"));
        assertThrows(JwtException.class, () -> jwtService.extractSubject(""));
    }

    // 8. Rate limiting (diferenciado por rota, fallback resiliente e bloqueio progressivo por ataque)
    @Test
    void shouldEnforceRateLimitsAndFallbackResilientlyWhenPrimaryStorageFails() {
        AtomicLong clock = new AtomicLong(1_000_000L);
        SecurityAuditLogger auditLogger = new SecurityAuditLogger();
        InMemoryRateLimitStorage fallback = new InMemoryRateLimitStorage(clock::get);
        RateLimitStorage failingPrimary = new RateLimitStorage() {
            @Override public String name() { return "postgres-down"; }
            @Override public boolean shared() { return true; }
            @Override public RateLimitWindow increment(String key, long windowMs) {
                throw new IllegalStateException("Connection refused");
            }
            @Override public void reset() {}
        };
        ResilientRateLimitStorage resilientStorage =
                new ResilientRateLimitStorage(failingPrimary, fallback, auditLogger);

        RateLimitService rateLimitService = new RateLimitService(
                resilientStorage,
                new InMemoryRateLimitStorage(clock::get),
                auditLogger,
                clock::get,
                3, 900_000L, 5, 900_000L,
                5, 10, 10, 30, 5, 60, 15, 60, 100, 10_000L, 600_000L
        );

        // 3 tentativas de login permitidas, 4ª bloqueada com 429
        assertTrue(rateLimitService.checkRequest("203.0.113.10", null, "POST", "/api/v1/auth/login").allowed());
        assertTrue(rateLimitService.checkRequest("203.0.113.10", null, "POST", "/api/v1/auth/login").allowed());
        assertTrue(rateLimitService.checkRequest("203.0.113.10", null, "POST", "/api/v1/auth/login").allowed());

        RateLimitService.RateLimitDecision fourth =
                rateLimitService.checkRequest("203.0.113.10", null, "POST", "/api/v1/auth/login");
        assertFalse(fourth.allowed());
        assertEquals(0, fourth.remaining());
        assertTrue(fourth.retryAfterSeconds() > 0);

        // Política 'so-a-rota': outra rota no mesmo IP compartilhado continua liberada
        assertTrue(rateLimitService.checkRequest("203.0.113.10", null, "GET", "/api/v1/public/restaurants/borae/menu").allowed());

        // Avançar o relógio além da janela de 15 minutos libera novamente a rota de login
        clock.addAndGet(901_000L);
        assertTrue(rateLimitService.checkRequest("203.0.113.10", null, "POST", "/api/v1/auth/login").allowed());
    }

    @Test
    void shouldApplyProgressiveIpBlockingOnAttackAndDetectWafThreats() {
        AtomicLong clock = new AtomicLong(2_000_000L);
        SecurityAuditLogger auditLogger = new SecurityAuditLogger();
        InMemoryRateLimitStorage storage = new InMemoryRateLimitStorage(clock::get);
        RateLimitService rateLimitService = new RateLimitService(
                storage, storage, auditLogger, clock::get,
                10, 900_000L, 5, 900_000L, 10, 15, 10, 30, 20, 120, 30, 120, 100, 10_000L, 60_000L
        );

        RateLimitService.IpBlock firstBlock = rateLimitService.blockIp("198.51.100.77", "sql_injection", 60_000L);
        assertEquals(1, firstBlock.recurrences());
        assertEquals(2_060_000L, firstBlock.untilEpochMs());

        RateLimitService.IpBlock secondBlock = rateLimitService.blockIp("198.51.100.77", "sql_injection", 60_000L);
        assertEquals(2, secondBlock.recurrences());
        assertEquals(2_120_000L, secondBlock.untilEpochMs());

        WafRequestInspector inspector = new WafRequestInspector();
        assertNotNull(inspector.inspectUserAgent("sqlmap/1.7.2"));
        assertNotNull(inspector.inspectUrl("/api/v1/public/restaurants/test", "q=' OR 1=1 --"));
        assertNotNull(inspector.inspectUrl("/api/v1/public/restaurants/..%2f..%2fetc%2fpasswd", null));
        assertNotNull(inspector.inspectUrl("/api/v1/public/restaurants/test", "name=<script>alert(1)</script>"));
        assertNull(inspector.inspectUrl("/api/v1/public/restaurants/cafe-sao-paulo/menu", null));
    }

    // 10. CORS não aceita wildcard
    @Test
    void shouldRejectWildcardInCorsAllowedOrigins() {
        SecurityConfig securityConfig = new SecurityConfig();
        assertThrows(IllegalStateException.class,
                () -> securityConfig.corsConfigurationSource("*"));
        assertThrows(IllegalStateException.class,
                () -> securityConfig.corsConfigurationSource("https://borae.com, *"));
    }

    // 11 & 12. Upload inválido e upload acima do limite
    @Test
    void shouldRejectInvalidAndOversizedUploads() {
        RestaurantImageProcessor imageProcessor = new RestaurantImageProcessor();
        // Conteúdo falso que não possui magic bytes JPEG/PNG/WebP
        assertThrows(ResponseStatusException.class,
                () -> imageProcessor.process("not-an-image-payload-12345".getBytes(StandardCharsets.UTF_8),
                        "fake.jpg", "image/jpeg"));
        // Path traversal no nome do arquivo
        assertThrows(ResponseStatusException.class,
                () -> imageProcessor.process(new byte[32], "../secret.png", "image/png"));

        // GLB com data URI malicioso (XSS / SVG)
        GlbFileValidator glbValidator = new GlbFileValidator(new ObjectMapper());
        byte[] maliciousGlbJson = "{\"asset\":{\"version\":\"2.0\"},\"images\":[{\"uri\":\"data:text/html,<script>alert(1)</script>\"}]}"
                .getBytes(StandardCharsets.UTF_8);
        int padded = (maliciousGlbJson.length + 3) & ~3;
        ByteBuffer glb = ByteBuffer.allocate(20 + padded).order(ByteOrder.LITTLE_ENDIAN);
        glb.putInt(0x46546C67).putInt(2).putInt(glb.capacity()).putInt(padded).putInt(0x4E4F534A).put(maliciousGlbJson);
        while (glb.position() < glb.capacity()) glb.put((byte) 0x20);
        assertThrows(ResponseStatusException.class, () -> glbValidator.validate(glb.array()));

        // Upload de logo acima de 5 MB -> 413 Payload Too Large
        RestaurantRepository restaurantRepo = mock(RestaurantRepository.class);
        RestaurantMembershipRepository membershipRepo = mock(RestaurantMembershipRepository.class);
        RestaurantMediaRepository mediaRepo = mock(RestaurantMediaRepository.class);
        RestaurantObjectStorage storage = mock(RestaurantObjectStorage.class);
        RestaurantMediaService mediaService = new RestaurantMediaService(
                restaurantRepo, membershipRepo, mediaRepo, storage, imageProcessor);

        UserAccount owner = new UserAccount("Owner", "owner@example.com", "hash");
        Restaurant restaurant = new Restaurant("Restaurante", "rest-123", null);
        when(membershipRepo.findByUser_IdAndRestaurant_IdAndActiveTrue(owner.getId(), restaurant.getId()))
                .thenReturn(Optional.of(new RestaurantMembership(owner, restaurant, RestaurantRole.OWNER)));

        MockMultipartFile oversizedLogo = new MockMultipartFile(
                "file", "logo.png", "image/png", new byte[5 * 1024 * 1024 + 10]);
        ResponseStatusException logoEx = assertThrows(ResponseStatusException.class,
                () -> mediaService.upload(restaurant.getId(), RestaurantMediaType.LOGO, oversizedLogo, owner));
        assertEquals(413, logoEx.getStatusCode().value());

        // Upload de modelo 3D acima de 10 MB -> 413 Payload Too Large
        MenuRepository menuRepo = mock(MenuRepository.class);
        MenuProductRepository productRepo = mock(MenuProductRepository.class);
        Product3DModelRepository modelRepo = mock(Product3DModelRepository.class);
        Product3DModelAuditRepository auditRepo = mock(Product3DModelAuditRepository.class);
        Product3DModelService modelService = new Product3DModelService(
                membershipRepo, menuRepo, productRepo, modelRepo, auditRepo, storage, glbValidator);

        Menu menu = new Menu(restaurant, "Menu", null, 0);
        MenuCategory category = new MenuCategory(menu, "Cat", null, 0);
        MenuProduct product = new MenuProduct(category, "Prod", null, 1000L, 0, List.of(), List.of());
        when(menuRepo.findByIdAndRestaurant_Id(menu.getId(), restaurant.getId())).thenReturn(Optional.of(menu));
        when(productRepo.findByIdAndCategory_Menu_Id(product.getId(), menu.getId())).thenReturn(Optional.of(product));

        MockMultipartFile oversizedGlb = new MockMultipartFile(
                "file", "dish.glb", "model/gltf-binary", new byte[10 * 1024 * 1024 + 10]);
        ResponseStatusException glbEx = assertThrows(ResponseStatusException.class,
                () -> modelService.upload(restaurant.getId(), menu.getId(), product.getId(),
                        oversizedGlb, 100, 100, 100, owner));
        assertEquals(413, glbEx.getStatusCode().value());
    }

    // 13. Erros sem vazamento de informações sensíveis
    @Test
    void shouldSanitizeDatabaseAndUnexpectedErrorsWithoutLeakingInternalDetails() {
        SecurityAuditLogger auditLogger = new SecurityAuditLogger();
        ClientIpResolver ipResolver = new ClientIpResolver("X-Forwarded-For", true);
        ApiExceptionHandler handler = new ApiExceptionHandler(auditLogger, ipResolver);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/restaurants");
        request.setRemoteAddr("192.0.2.50");

        DataIntegrityViolationException dbException = new DataIntegrityViolationException(
                "ERROR: duplicate key value violates unique constraint \"uk_restaurant_slug\" Detail: Key (slug)=(secret) already exists; SELECT * FROM user_account WHERE password_hash='secret'",
                new SQLException("SQL state 23505 on /var/lib/postgresql/data")
        );

        ResponseEntity<Map<String, Object>> response = handler.handleDataAccessException(dbException, request);
        assertEquals(500, response.getStatusCode().value());
        String message = String.valueOf(response.getBody().get("message"));
        assertEquals("Erro interno do servidor.", message);
        assertFalse(message.contains("SELECT"));
        assertFalse(message.contains("uk_restaurant_slug"));
        assertFalse(message.contains("postgresql"));

        ResponseEntity<Map<String, Object>> maxUploadResponse =
                handler.handleMaxUploadSizeExceeded(new MaxUploadSizeExceededException(10_485_760L), request);
        assertEquals(413, maxUploadResponse.getStatusCode().value());

        String redacted = SecurityAuditLogger.redactSensitive(
                "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.sig password=mySecretPassword123");
        assertFalse(redacted.contains("eyJhbGciOiJIUzI1NiJ9"));
        assertFalse(redacted.contains("mySecretPassword123"));
    }

    // 14. Inicialização com configuração de produção incompleta
    @Test
    void shouldFailStartupWhenProductionConfigurationIsIncompleteOrInsecure() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        // Senha de banco padrão "change-me"
        ProductionSecurityConfigValidator defaultDbPasswordValidator = new ProductionSecurityConfigValidator(
                prodEnv,
                "jdbc:postgresql://ep-neon-pooler.us-east-2.aws.neon.tech:5432/borae",
                "borae_prod",
                "change-me",
                "https://borae-web.onrender.com",
                "https://borae-web.onrender.com/r/",
                VALID_SECRET
        );
        assertThrows(IllegalStateException.class, defaultDbPasswordValidator::validateOnStartup);

        // DB_URL apontando para localhost em produção
        ProductionSecurityConfigValidator localhostDbValidator = new ProductionSecurityConfigValidator(
                prodEnv,
                "jdbc:postgresql://localhost:5432/borae",
                "borae_prod",
                "StrongProdDbPass#2026!",
                "https://borae-web.onrender.com",
                "https://borae-web.onrender.com/r/",
                VALID_SECRET
        );
        assertThrows(IllegalStateException.class, localhostDbValidator::validateOnStartup);

        // CORS com HTTP inseguro / localhost em produção
        ProductionSecurityConfigValidator insecureCorsValidator = new ProductionSecurityConfigValidator(
                prodEnv,
                "jdbc:postgresql://ep-neon-pooler.us-east-2.aws.neon.tech:5432/borae",
                "borae_prod",
                "StrongProdDbPass#2026!",
                "http://localhost:4200",
                "https://borae-web.onrender.com/r/",
                VALID_SECRET
        );
        assertThrows(IllegalStateException.class, insecureCorsValidator::validateOnStartup);

        // PUBLIC_RESTAURANT_URL_PREFIX sem HTTPS em produção
        ProductionSecurityConfigValidator insecurePublicUrlValidator = new ProductionSecurityConfigValidator(
                prodEnv,
                "jdbc:postgresql://ep-neon-pooler.us-east-2.aws.neon.tech:5432/borae",
                "borae_prod",
                "StrongProdDbPass#2026!",
                "https://borae-web.onrender.com",
                "http://borae-web.onrender.com/r/",
                VALID_SECRET
        );
        assertThrows(IllegalStateException.class, insecurePublicUrlValidator::validateOnStartup);

        // Configuração de produção completa e segura -> inicializa com sucesso
        ProductionSecurityConfigValidator validProdValidator = new ProductionSecurityConfigValidator(
                prodEnv,
                "jdbc:postgresql://ep-neon-pooler.us-east-2.aws.neon.tech:5432/borae",
                "borae_prod",
                "StrongProdDbPass#2026!",
                "https://borae-web.onrender.com",
                "https://borae-web.onrender.com/r/",
                VALID_SECRET
        );
        validProdValidator.validateOnStartup();
    }
}
