package com.borae.security;

import com.borae.auth.AuthService;
import com.borae.auth.dto.LoginRequest;
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
import com.borae.identity.UserAccountRepository;
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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
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
        assertThrows(IllegalStateException.class, () -> new JwtService("%%%not-base64%%%", 900_000L));
        assertThrows(IllegalStateException.class, () -> new JwtService("YWJjZGVmZ2hpamtsbW5vcA==", 900_000L));
        assertThrows(IllegalStateException.class,
                () -> new JwtService("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=", 900_000L));
        assertThrows(IllegalStateException.class,
                () -> new JwtService("QUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUFBQUE=", 900_000L));
    }

    // 3 & 4 (SEC-REV-01). Validação obrigatória de issuer, exp, sub, assinatura e limite de tamanho do JWT
    @Test
    void shouldRequireIssuerAndValidateAllMandatoryJwtClaims() {
        JwtService jwtService = new JwtService(VALID_SECRET, 900_000L, "borae-api");
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(VALID_SECRET));
        Instant now = Instant.now();

        // 1. JWT válido com iss=borae-api -> aceito
        String validToken = jwtService.generateToken("user@example.com");
        assertEquals("user@example.com", jwtService.extractSubject(validToken));

        // 2. JWT sem claim 'iss' -> REJEITADO (SEC-REV-01)
        String tokenWithoutIssuer = Jwts.builder()
                .subject("user@example.com")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        assertThrows(JwtException.class, () -> jwtService.extractSubject(tokenWithoutIssuer));

        // 3. JWT com issuer diferente -> REJEITADO
        JwtService wrongIssuerService = new JwtService(VALID_SECRET, 900_000L, "other-issuer");
        String wrongIssuerToken = wrongIssuerService.generateToken("user@example.com");
        assertThrows(JwtException.class, () -> jwtService.extractSubject(wrongIssuerToken));

        // 4. JWT sem claim 'exp' -> REJEITADO
        String tokenWithoutExp = Jwts.builder()
                .subject("user@example.com")
                .issuer("borae-api")
                .issuedAt(Date.from(now))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        assertThrows(JwtException.class, () -> jwtService.extractSubject(tokenWithoutExp));

        // 5. JWT expirado -> REJEITADO
        Instant past = now.minusSeconds(600);
        String expiredToken = Jwts.builder()
                .subject("user@example.com")
                .issuer("borae-api")
                .issuedAt(Date.from(past.minusSeconds(900)))
                .expiration(Date.from(past))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        assertThrows(JwtException.class, () -> jwtService.extractSubject(expiredToken));

        // 6. JWT sem claim 'sub' -> REJEITADO
        String tokenWithoutSub = Jwts.builder()
                .issuer("borae-api")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(900)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        assertThrows(JwtException.class, () -> jwtService.extractSubject(tokenWithoutSub));

        // 7. JWT com assinatura inválida -> REJEITADO
        JwtService attackerJwtService = new JwtService(OTHER_VALID_SECRET, 900_000L, "borae-api");
        String forgedToken = attackerJwtService.generateToken("admin@example.com");
        assertThrows(JwtException.class, () -> jwtService.extractSubject(forgedToken));

        // 8. JWT acima do limite de tamanho (> 4096 chars) -> REJEITADO
        String oversizedToken = validToken + "a".repeat(4100);
        assertThrows(JwtException.class, () -> jwtService.extractSubject(oversizedToken));
    }

    // SEC-REV-02: Resolução segura do IP do cliente e validação da cadeia de proxies confiáveis
    @Test
    void shouldIgnoreSpoofedForwardedHeadersFromUntrustedConnectionsAndParseTrustedProxyChainsRightToLeft() {
        SecurityAuditLogger auditLogger = new SecurityAuditLogger();

        // 1. Sem proxies confiáveis configurados (padrão seguro fail-closed): ignora X-Forwarded-For, X-Real-IP e CF-Connecting-IP
        ClientIpResolver untrustedDefaultResolver = new ClientIpResolver(
                "X-Forwarded-For,X-Real-IP,CF-Connecting-IP", "", auditLogger);

        MockHttpServletRequest spoofXff = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        spoofXff.setRemoteAddr("198.51.100.42");
        spoofXff.addHeader("X-Forwarded-For", "203.0.113.1");
        assertEquals("198.51.100.42", untrustedDefaultResolver.resolve(spoofXff));

        MockHttpServletRequest spoofXRealIp = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        spoofXRealIp.setRemoteAddr("198.51.100.42");
        spoofXRealIp.addHeader("X-Real-IP", "203.0.113.2");
        assertEquals("198.51.100.42", untrustedDefaultResolver.resolve(spoofXRealIp));

        MockHttpServletRequest spoofCfIp = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        spoofCfIp.setRemoteAddr("198.51.100.42");
        spoofCfIp.addHeader("CF-Connecting-IP", "203.0.113.3");
        assertEquals("198.51.100.42", untrustedDefaultResolver.resolve(spoofCfIp));

        // 2. Com proxies confiáveis configurados (10.0.0.0/8 e 2001:db8:abcd::/48)
        ClientIpResolver trustedProxyResolver = new ClientIpResolver(
                "X-Forwarded-For,X-Real-IP,CF-Connecting-IP",
                "10.0.0.0/8, 172.16.0.0/12, 2001:db8:abcd::/48",
                auditLogger
        );

        // Conexão direta de IP externo não confiável tentando falsificar X-Forwarded-For -> ignorado!
        MockHttpServletRequest externalDirect = new MockHttpServletRequest("GET", "/api/v1/public/restaurants/borae/menu");
        externalDirect.setRemoteAddr("203.0.113.99");
        externalDirect.addHeader("X-Forwarded-For", "1.1.1.1");
        assertEquals("203.0.113.99", trustedProxyResolver.resolve(externalDirect));

        // Conexão vinda de proxy confiável (10.0.1.5) com 1 cliente real
        MockHttpServletRequest singleTrustedProxy = new MockHttpServletRequest("GET", "/api/v1/users/me");
        singleTrustedProxy.setRemoteAddr("10.0.1.5");
        singleTrustedProxy.addHeader("X-Forwarded-For", "198.51.100.77");
        assertEquals("198.51.100.77", trustedProxyResolver.resolve(singleTrustedProxy));

        // Cadeia com múltiplos proxies confiáveis + tentativa de spoofing pelo cliente na esquerda:
        // X-Forwarded-For: 1.2.3.4 (injetado pelo cliente), 198.51.100.88 (cliente real adicionado pelo proxy 1), 172.16.0.10 (proxy 1 adicionado pelo proxy 2)
        MockHttpServletRequest multiProxyChain = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        multiProxyChain.setRemoteAddr("10.0.2.20");
        multiProxyChain.addHeader("X-Forwarded-For", "1.2.3.4, 198.51.100.88, 172.16.0.10");
        assertEquals("198.51.100.88", trustedProxyResolver.resolve(multiProxyChain));

        // IPv6 válido através de proxy confiável IPv6
        MockHttpServletRequest ipv6Request = new MockHttpServletRequest("GET", "/api/v1/users/me");
        ipv6Request.setRemoteAddr("2001:db8:abcd:0001::1");
        ipv6Request.addHeader("X-Forwarded-For", "2001:db8:1234::5678");
        assertEquals("2001:db8:1234:0:0:0:0:5678", trustedProxyResolver.resolve(ipv6Request));

        // Cadeia malformada (contém hostname ou texto inválido) -> rejeita cabeçalho e faz fallback para o IP do socket
        MockHttpServletRequest malformedChain = new MockHttpServletRequest("GET", "/api/v1/users/me");
        malformedChain.setRemoteAddr("10.0.1.5");
        malformedChain.addHeader("X-Forwarded-For", "invalid-host.example.com, 198.51.100.88");
        assertEquals("10.0.1.5", trustedProxyResolver.resolve(malformedChain));

        // Cabeçalho excessivamente grande (> 512 chars) -> rejeitado com fallback para socket
        MockHttpServletRequest oversizedHeader = new MockHttpServletRequest("GET", "/api/v1/users/me");
        oversizedHeader.setRemoteAddr("10.0.1.5");
        oversizedHeader.addHeader("X-Forwarded-For", "198.51.100.1, ".repeat(50) + "198.51.100.2");
        assertEquals("10.0.1.5", trustedProxyResolver.resolve(oversizedHeader));

        // Ausência de cabeçalhos -> fallback para socket
        MockHttpServletRequest noHeaders = new MockHttpServletRequest("GET", "/api/v1/users/me");
        noHeaders.setRemoteAddr("10.0.1.5");
        assertEquals("10.0.1.5", trustedProxyResolver.resolve(noHeaders));
    }

    // SEC-REV-03: Bloqueio temporário após 5 falhas consecutivas de login, expiração, reset e anti-enumeração
    @Test
    void shouldEnforceLoginLockoutAfterFiveConsecutiveFailuresAndResetOnValidLoginOrExpiration() {
        AtomicLong clock = new AtomicLong(5_000_000L);
        SecurityAuditLogger auditLogger = new SecurityAuditLogger();
        List<String>recordedKeys = new ArrayList<>();
        InMemoryRateLimitStorage innerStorage = new InMemoryRateLimitStorage(clock::get);
        RateLimitStorage inspectingStorage = new RateLimitStorage() {
            @Override public String name() { return innerStorage.name(); }
            @Override public boolean shared() { return true; }
            @Override public RateLimitWindow increment(String key, long windowMs) {
                recordedKeys.add(key);
                return innerStorage.increment(key, windowMs);
            }
            @Override public Optional<RateLimitWindow> getActiveWindow(String key) {
                return innerStorage.getActiveWindow(key);
            }
            @Override public void remove(String key) {
                innerStorage.remove(key);
            }
            @Override public void reset() {
                innerStorage.reset();
            }
        };

        RateLimitService rateLimitService = new RateLimitService(
                inspectingStorage,
                innerStorage,
                auditLogger,
                clock::get,
                20, 900_000L, 5, 900_000L,
                10, 15, 10, 30, 20, 120, 30, 120, 100, 10_000L, 600_000L
        );

        PasswordEncoder encoder = new BCryptPasswordEncoder();
        UserAccountRepository userRepo = mock(UserAccountRepository.class);
        String existingEmail = "chef@restaurante.com.br";
        UserAccount activeUser = new UserAccount("Chef", existingEmail, encoder.encode("SenhaCorreta#2026"));
        when(userRepo.findByEmail(existingEmail)).thenReturn(Optional.of(activeUser));
        when(userRepo.findByEmail("naoexiste@restaurante.com.br")).thenReturn(Optional.empty());

        JwtService jwtService = new JwtService(VALID_SECRET, 900_000L, "borae-api");
        AuthService authService = new AuthService(userRepo, encoder, jwtService, rateLimitService, auditLogger);

        // 1 a 5: Cinco falhas consecutivas para e-mail existente -> 401 Unauthorized em todas as 5
        for (int attempt = 1; attempt <= 5; attempt++) {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> authService.login(new LoginRequest(existingEmail, "SenhaErrada#" + UUID.randomUUID()), "198.51.100.10"));
            assertEquals(401, ex.getStatusCode().value());
            assertEquals("E-mail ou senha inválidos.", ex.getReason());
        }

        // Verificar que nenhuma chave persistida contém o e-mail em texto puro ou a senha
        assertFalse(recordedKeys.isEmpty());
        for (String key : recordedKeys) {
            assertFalse(key.contains("chef"));
            assertFalse(key.contains("restaurante.com.br"));
            assertFalse(key.contains("Senha"));
        }

        // 6: Sexta tentativa durante a janela de bloqueio -> RateLimitExceededException (HTTP 429) mesmo com a senha correta!
        RateLimitExceededException lockedEx = assertThrows(RateLimitExceededException.class,
                () -> authService.login(new LoginRequest(existingEmail, "SenhaCorreta#2026"), "198.51.100.10"));
        assertEquals(5, lockedEx.getLimit());
        assertTrue(lockedEx.getRetryAfterSeconds() > 800);

        // 8: Avançar o relógio além dos 15 minutos (900.000 ms) -> bloqueio expira automaticamente (sem bloqueio permanente)
        clock.addAndGet(901_000L);
        var successAfterExpiry = authService.login(new LoginRequest(existingEmail, "SenhaCorreta#2026"), "198.51.100.10");
        assertNotNull(successAfterExpiry.accessToken());

        // 9: Política de reset em login válido: 3 falhas seguidas de 1 login válido zeram a contagem
        for (int i = 0; i < 3; i++) {
            assertThrows(ResponseStatusException.class,
                    () -> authService.login(new LoginRequest(existingEmail, "Errada"), "198.51.100.10"));
        }
        assertNotNull(authService.login(new LoginRequest(existingEmail, "SenhaCorreta#2026"), "198.51.100.10").accessToken());
        // Mais 4 falhas agora NÃO bloqueiam na 4ª porque o contador foi resetado no login válido anterior
        for (int i = 0; i < 4; i++) {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> authService.login(new LoginRequest(existingEmail, "Errada"), "198.51.100.10"));
            assertEquals(401, ex.getStatusCode().value());
        }

        // 10 & 12: E-mail inexistente tem exatamente o mesmo comportamento (5 falhas -> 401; 6ª tentativa -> 429)
        for (int attempt = 1; attempt <= 5; attempt++) {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> authService.login(new LoginRequest("naoexiste@restaurante.com.br", "QualquerSenha"), "198.51.100.20"));
            assertEquals(401, ex.getStatusCode().value());
            assertEquals("E-mail ou senha inválidos.", ex.getReason());
        }
        RateLimitExceededException nonExistentLocked = assertThrows(RateLimitExceededException.class,
                () -> authService.login(new LoginRequest("naoexiste@restaurante.com.br", "QualquerSenha"), "198.51.100.20"));
        assertEquals(lockedEx.getLimit(), nonExistentLocked.getLimit());
        assertEquals(lockedEx.getRuleName(), nonExistentLocked.getRuleName());
    }

    // 8 & 14: Fallback resiliente durante indisponibilidade do PostgreSQL com throttling de logs
    @Test
    void shouldEnforceRateLimitsAndThrottleLogsWhenPrimaryPostgresStorageFailsAndRecovers() {
        AtomicLong clock = new AtomicLong(1_000_000L);
        AtomicBoolean primaryDown = new AtomicBoolean(true);
        SecurityAuditLogger auditLogger = new SecurityAuditLogger();
        InMemoryRateLimitStorage primary = new InMemoryRateLimitStorage(clock::get);
        InMemoryRateLimitStorage fallback = new InMemoryRateLimitStorage(clock::get);

        RateLimitStorage toggleablePrimary = new RateLimitStorage() {
            @Override public String name() { return "postgres"; }
            @Override public boolean shared() { return true; }
            @Override public RateLimitWindow increment(String key, long windowMs) {
                if (primaryDown.get()) throw new IllegalStateException("PostgreSQL connection timeout");
                return primary.increment(key, windowMs);
            }
            @Override public Optional<RateLimitWindow> getActiveWindow(String key) {
                if (primaryDown.get()) throw new IllegalStateException("PostgreSQL connection timeout");
                return primary.getActiveWindow(key);
            }
            @Override public void remove(String key) {
                if (primaryDown.get()) throw new IllegalStateException("PostgreSQL connection timeout");
                primary.remove(key);
            }
            @Override public void reset() { primary.reset(); }
        };

        ResilientRateLimitStorage resilientStorage =
                new ResilientRateLimitStorage(toggleablePrimary, fallback, auditLogger, clock::get);

        RateLimitService rateLimitService = new RateLimitService(
                resilientStorage,
                new InMemoryRateLimitStorage(clock::get),
                auditLogger,
                clock::get,
                3, 900_000L, 5, 900_000L,
                5, 10, 10, 30, 5, 60, 15, 60, 100, 10_000L, 600_000L
        );

        // 5 falhas de login com o PostgreSQL indisponível degradam para a memória local e bloqueiam a 6ª tentativa
        for (int i = 0; i < 5; i++) {
            rateLimitService.recordLoginFailure("degraded@example.com", "203.0.113.10");
        }
        assertThrows(RateLimitExceededException.class,
                () -> rateLimitService.assertLoginAttemptAllowed("degraded@example.com", "203.0.113.10"));

        // Throttling de log: apesar de > 10 operações com falha no mesmo minuto, apenas 1 evento rate_limit_fallback foi emitido
        long fallbackLogs = auditLogger.getRecentEvents().stream()
                .filter(e -> "rate_limit_fallback".equals(e.eventType()))
                .count();
        assertEquals(1L, fallbackLogs);

        // Quando o PostgreSQL volta a responder, registra o evento rate_limit_recovered
        primaryDown.set(false);
        rateLimitService.recordLoginFailure("recovered@example.com", "203.0.113.11");
        assertTrue(auditLogger.getRecentEvents().stream()
                .anyMatch(e -> "rate_limit_recovered".equals(e.eventType())));
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
        assertThrows(ResponseStatusException.class,
                () -> imageProcessor.process("not-an-image-payload-12345".getBytes(StandardCharsets.UTF_8),
                        "fake.jpg", "image/jpeg"));
        assertThrows(ResponseStatusException.class,
                () -> imageProcessor.process(new byte[32], "../secret.png", "image/png"));

        GlbFileValidator glbValidator = new GlbFileValidator(new ObjectMapper());
        byte[] maliciousGlbJson = "{\"asset\":{\"version\":\"2.0\"},\"images\":[{\"uri\":\"data:text/html,<script>alert(1)</script>\"}]}"
                .getBytes(StandardCharsets.UTF_8);
        int padded = (maliciousGlbJson.length + 3) & ~3;
        ByteBuffer glb = ByteBuffer.allocate(20 + padded).order(ByteOrder.LITTLE_ENDIAN);
        glb.putInt(0x46546C67).putInt(2).putInt(glb.capacity()).putInt(padded).putInt(0x4E4F534A).put(maliciousGlbJson);
        while (glb.position() < glb.capacity()) glb.put((byte) 0x20);
        assertThrows(ResponseStatusException.class, () -> glbValidator.validate(glb.array()));

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
        ClientIpResolver ipResolver = new ClientIpResolver("X-Forwarded-For", "");
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
