package com.borae.security;

import com.borae.auth.AuthController;
import com.borae.auth.AuthService;
import com.borae.auth.dto.LoginRequest;
import com.borae.config.ApiExceptionHandler;
import com.borae.identity.UserAccountRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes de integração com PostgreSQL 16 (via Testcontainers no CI ou instância PostgreSQL 16 dedicada)
 * percorrendo o fluxo real de autenticação ({@code POST /api/v1/auth/login} -> {@link BoraeSecurityShieldFilter}
 * -> {@link AuthController} -> {@link AuthService} -> {@link RateLimitService} -> {@link PostgresRateLimitStorage}).
 *
 * <p>Cobre os 5 cenários obrigatórios da Sessão 01.2:
 * <ul>
 *   <li>Teste A — Persistência no PostgreSQL após HTTP 401</li>
 *   <li>Teste B — Bloqueio distribuído entre duas instâncias com memórias locais independentes</li>
 *   <li>Teste C — Ausência de rollback do contador quando a autenticação falha com HTTP 401</li>
 *   <li>Teste D — Reset distribuído no PostgreSQL após login válido</li>
 *   <li>Teste E — Falha real do PostgreSQL com fallback local, alerta operacional e recuperação automática</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoginPostgresDistributedIntegrationTest {

    private static final String TARGET_EMAIL = "transactional-login@borae.com";
    private static final String VALID_PASSWORD = "StrongPassword@2026";
    private static final String WRONG_PASSWORD = "WrongPassword!999";

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
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthService authService;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private SecurityAuditLogger auditLogger;

    @Autowired
    private ClientIpResolver clientIpResolver;

    @Autowired
    private ApiExceptionHandler apiExceptionHandler;

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @BeforeEach
    void setUp() throws Exception {
        rateLimitService.resetAll();
        auditLogger.clearEvents();

        if (!userAccountRepository.existsByEmail(TARGET_EMAIL)) {
            mockMvc.perform(post("/api/v1/auth/register")
                            .with(req -> {
                                req.setRemoteAddr("198.51.100.1");
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "fullName", "Transactional Test User",
                                    "email", TARGET_EMAIL,
                                    "password", VALID_PASSWORD
                            ))))
                    .andExpect(status().isCreated());
        }

        rateLimitService.resetAll();
        auditLogger.clearEvents();
    }

    /**
     * Teste A — Persistência após HTTP 401:
     * Envia uma tentativa de login com senha incorreta pelo fluxo HTTP real,
     * confirma HTTP 401, consulta diretamente {@code security_rate_limit} no PostgreSQL
     * e comprova que o contador foi gravado e confirmado no banco sem fallback em memória.
     */
    @Test
    void testA_persistsCounterInPostgresAfterHttp401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.10");
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", TARGET_EMAIL,
                                "password", WRONG_PASSWORD
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        Integer accountFailureCount = jdbcTemplate.queryForObject(
                "SELECT request_count FROM security_rate_limit WHERE bucket_key LIKE 'login-fail:%'",
                Integer.class
        );
        Integer ipAccountFailureCount = jdbcTemplate.queryForObject(
                "SELECT request_count FROM security_rate_limit WHERE bucket_key LIKE 'login-ip-fail:%'",
                Integer.class
        );
        Integer routeIpCount = jdbcTemplate.queryForObject(
                "SELECT request_count FROM security_rate_limit WHERE bucket_key = 'anon:198.51.100.10:auth-login-ip'",
                Integer.class
        );

        assertEquals(1, accountFailureCount, "O contador por conta (login-fail) deve estar persistido no PostgreSQL após HTTP 401.");
        assertEquals(1, ipAccountFailureCount, "O contador por IP+conta (login-ip-fail) deve estar persistido no PostgreSQL após HTTP 401.");
        assertEquals(1, routeIpCount, "O contador volumétrico da rota de login deve estar persistido no PostgreSQL.");

        List<String> persistedKeys = jdbcTemplate.queryForList(
                "SELECT bucket_key FROM security_rate_limit",
                String.class
        );
        assertFalse(persistedKeys.isEmpty());
        for (String key : persistedKeys) {
            assertFalse(key.contains(TARGET_EMAIL), "Nenhum e-mail em texto puro pode ser persistido em bucket_key: " + key);
            assertFalse(key.contains(WRONG_PASSWORD), "Nenhuma senha pode ser persistida em bucket_key.");
        }

        boolean hadFallbackEvent = auditLogger.getRecentEvents().stream()
                .anyMatch(e -> "rate_limit_fallback".equals(e.eventType()));
        assertFalse(hadFallbackEvent, "Não deve ocorrer fallback em memória quando o PostgreSQL está operacional.");
    }

    /**
     * Teste B — Bloqueio entre instâncias:
     * Instância A e Instância B compartilham o mesmo PostgreSQL, mas possuem
     * armazenamentos locais em memória completamente independentes.
     * Registra 3 falhas via Instância A e 2 falhas via Instância B (todas HTTP 401),
     * e confirma que a 6ª tentativa retorna HTTP 429 em ambas as instâncias via PostgreSQL.
     */
    @Test
    void testB_enforcesSharedLockoutAcrossTwoApplicationInstancesViaPostgres() throws Exception {
        // Instância B possui seu próprio RateLimitService e seu próprio InMemoryRateLimitStorage independente,
        // compartilhando apenas o PostgreSQL (JdbcTemplate + PlatformTransactionManager).
        RateLimitService instanceBRateLimitService = new RateLimitService(
                jdbcTemplate,
                transactionManager,
                auditLogger,
                "postgres",
                jwtSecret,
                20, 900_000L, 5, 900_000L, 10, 15, 10, 30, 20, 120, 30, 120, 100, 10_000L, 600_000L
        );
        ClientIpResolver instanceBIpResolver = new ClientIpResolver("", false);
        AuthService instanceBAuthService = new AuthService(
                userAccountRepository,
                passwordEncoder,
                jwtService,
                instanceBRateLimitService,
                auditLogger
        );
        AuthController instanceBAuthController = new AuthController(instanceBAuthService, instanceBIpResolver);
        BoraeSecurityShieldFilter instanceBShieldFilter = new BoraeSecurityShieldFilter(
                instanceBIpResolver,
                new WafRequestInspector(),
                instanceBRateLimitService,
                new SecurityAnomalyMonitor(),
                auditLogger,
                objectMapper,
                false
        );
        MockMvc mockMvcInstanceB = MockMvcBuilders.standaloneSetup(instanceBAuthController)
                .setControllerAdvice(apiExceptionHandler)
                .addFilters(instanceBShieldFilter)
                .build();

        // 1. Registrar 3 falhas por meio da Instância A (HTTP 401)
        for (int i = 1; i <= 3; i++) {
            final String ip = "198.51.100." + (20 + i);
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(req -> {
                                req.setRemoteAddr(ip);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "email", TARGET_EMAIL,
                                    "password", WRONG_PASSWORD
                            ))))
                    .andExpect(status().isUnauthorized());
        }

        assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT request_count FROM security_rate_limit WHERE bucket_key LIKE 'login-fail:%'",
                Integer.class
        ));

        // 2. Registrar 2 falhas por meio da Instância B (HTTP 401)
        for (int i = 1; i <= 2; i++) {
            final String ip = "198.51.100." + (30 + i);
            mockMvcInstanceB.perform(post("/api/v1/auth/login")
                            .with(req -> {
                                req.setRemoteAddr(ip);
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "email", TARGET_EMAIL,
                                    "password", WRONG_PASSWORD
                            ))))
                    .andExpect(status().isUnauthorized());
        }

        // Confirmar que o PostgreSQL acumulou as 5 falhas (3 da Instância A + 2 da Instância B)
        assertEquals(5, jdbcTemplate.queryForObject(
                "SELECT request_count FROM security_rate_limit WHERE bucket_key LIKE 'login-fail:%'",
                Integer.class
        ));

        // Confirmar que a memória local não foi utilizada para o bloqueio
        assertFalse(auditLogger.getRecentEvents().stream()
                .anyMatch(e -> "rate_limit_fallback".equals(e.eventType())));

        // 3. Realizar a 6ª tentativa na Instância A e na Instância B -> ambas devem devolver HTTP 429
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.40");
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", TARGET_EMAIL,
                                "password", VALID_PASSWORD
                        ))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("X-RateLimit-Limit", "5"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"));

        mockMvcInstanceB.perform(post("/api/v1/auth/login")
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.41");
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", TARGET_EMAIL,
                                "password", VALID_PASSWORD
                        ))))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(header().string("X-RateLimit-Limit", "5"))
                .andExpect(header().string("X-RateLimit-Remaining", "0"));
    }

    /**
     * Teste C — Rollback:
     * Simula autenticações inválidas que terminam em exceção HTTP 401 (inclusive quando chamadas
     * dentro de uma transação externa {@code readOnly} ou de escrita que sofre rollback) e confirma
     * que a contagem de falhas no PostgreSQL nunca é revertida e permanece disponível para a próxima requisição.
     */
    @Test
    void testC_authenticationFailureAndOuterTransactionRollbackDoNotRevertPersistedCounter() throws Exception {
        // 1. Falha via endpoint HTTP real (termina em ResponseStatusException 401)
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.50");
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", TARGET_EMAIL,
                                "password", WRONG_PASSWORD
                        ))))
                .andExpect(status().isUnauthorized());

        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT request_count FROM security_rate_limit WHERE bucket_key LIKE 'login-fail:%'",
                Integer.class
        ));

        // 2. Simular chamada de AuthService.login dentro de uma transação externa read-only que sofre rollback ao lançar 401
        TransactionTemplate outerReadOnlyTx = new TransactionTemplate(transactionManager);
        outerReadOnlyTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        outerReadOnlyTx.setReadOnly(true);

        ResponseStatusException ex1 = assertThrows(ResponseStatusException.class, () ->
                outerReadOnlyTx.execute(status ->
                        authService.login(new LoginRequest(TARGET_EMAIL, WRONG_PASSWORD), "198.51.100.51")
                )
        );
        assertEquals(401, ex1.getStatusCode().value());

        // Confirmar que o incremento para 2 foi confirmado em transação REQUIRES_NEW e não sofreu rollback
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT request_count FROM security_rate_limit WHERE bucket_key LIKE 'login-fail:%'",
                Integer.class
        ));

        // 3. Simular chamada dentro de uma transação externa de leitura/escrita marcada para rollback
        TransactionTemplate outerReadWriteTx = new TransactionTemplate(transactionManager);
        outerReadWriteTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        outerReadWriteTx.setReadOnly(false);

        ResponseStatusException ex2 = assertThrows(ResponseStatusException.class, () ->
                outerReadWriteTx.execute(status ->
                        authService.login(new LoginRequest(TARGET_EMAIL, WRONG_PASSWORD), "198.51.100.52")
                )
        );
        assertEquals(401, ex2.getStatusCode().value());

        // O registro permanece disponível e atualizado (count = 3) para a próxima requisição no PostgreSQL
        assertEquals(3, jdbcTemplate.queryForObject(
                "SELECT request_count FROM security_rate_limit WHERE bucket_key LIKE 'login-fail:%'",
                Integer.class
        ));
    }

    /**
     * Teste D — Reset após login válido:
     * Registra falhas suficientes para ativar o bloqueio no PostgreSQL, realiza autenticação válida
     * quando permitido pelo cenário (após liberação da janela de bloqueio), confirma a remoção
     * dos contadores em {@code security_rate_limit} no PostgreSQL e comprova que uma segunda instância
     * enxerga imediatamente o estado limpo.
     */
    @Test
    void testD_validLoginClearsCountersInPostgresAndUnblocksOtherInstances() throws Exception {
        RateLimitService instanceBRateLimitService = new RateLimitService(
                jdbcTemplate,
                transactionManager,
                auditLogger,
                "postgres",
                jwtSecret,
                20, 900_000L, 5, 900_000L, 10, 15, 10, 30, 20, 120, 30, 120, 100, 10_000L, 600_000L
        );

        // 1. Registrar 5 falhas para ativar o bloqueio no PostgreSQL
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .with(req -> {
                                req.setRemoteAddr("198.51.100.60");
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "email", TARGET_EMAIL,
                                    "password", WRONG_PASSWORD
                            ))))
                    .andExpect(status().isUnauthorized());
        }

        // Confirmar que o bloqueio está ativo no PostgreSQL e visível na Instância B
        assertEquals(5, jdbcTemplate.queryForObject(
                "SELECT request_count FROM security_rate_limit WHERE bucket_key LIKE 'login-fail:%'",
                Integer.class
        ));
        assertThrows(RateLimitExceededException.class,
                () -> instanceBRateLimitService.assertLoginAttemptAllowed(TARGET_EMAIL, "198.51.100.60"));

        // 2. Simular cenário em que o login válido é permitido (mantendo os registros na tabela security_rate_limit
        // para comprovar que clearLoginFailures executa o DELETE efetivo no PostgreSQL)
        jdbcTemplate.update(
                "UPDATE security_rate_limit SET request_count = 4 WHERE bucket_key LIKE 'login-fail:%' OR bucket_key LIKE 'login-ip-fail:%'"
        );
        assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM security_rate_limit WHERE bucket_key LIKE 'login-fail:%' OR bucket_key LIKE 'login-ip-fail:%'",
                Integer.class
        ));

        // 3. Realizar autenticação válida via MockMvc (Instância A)
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.60");
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", TARGET_EMAIL,
                                "password", VALID_PASSWORD
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());

        // 4. Confirmar a remoção completa dos contadores de falha de login no PostgreSQL
        Integer remainingFailureCounters = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM security_rate_limit WHERE bucket_key LIKE 'login-fail:%' OR bucket_key LIKE 'login-ip-fail:%'",
                Integer.class
        );
        assertEquals(0, remainingFailureCounters, "Os contadores de falha devem ser removidos do PostgreSQL após login válido.");

        // 5. Confirmar que a Instância B consulta o PostgreSQL e vê o estado limpo
        instanceBRateLimitService.assertLoginAttemptAllowed(TARGET_EMAIL, "198.51.100.60");
    }

    /**
     * Teste E — Falha real do PostgreSQL:
     * Simula indisponibilidade real de conexão com o PostgreSQL primário,
     * confirma a ativação do fallback local em memória durante o fluxo de login,
     * confirma o registro do alerta operacional {@code rate_limit_fallback},
     * confirma a recuperação automática ({@code rate_limit_recovered}) quando o PostgreSQL volta,
     * e comprova que erros de configuração transacional (SQLState 25006 read-only) não são mascarados.
     */
    @Test
    void testE_realPostgresOutageActivatesLocalFallbackLogsAlertAndRecovers() throws Exception {
        PostgresRateLimitStorage realPostgresStorage = new PostgresRateLimitStorage(jdbcTemplate, transactionManager);
        InMemoryRateLimitStorage fallbackMemoryStorage = new InMemoryRateLimitStorage();
        AtomicBoolean postgresAvailable = new AtomicBoolean(false);

        RateLimitStorage toggleablePrimary = new RateLimitStorage() {
            @Override
            public String name() {
                return realPostgresStorage.name();
            }

            @Override
            public boolean shared() {
                return realPostgresStorage.shared();
            }

            @Override
            public RateLimitWindow increment(String key, long windowMs) {
                if (!postgresAvailable.get()) {
                    throw new CannotGetJdbcConnectionException("Simulated PostgreSQL primary connection failure");
                }
                return realPostgresStorage.increment(key, windowMs);
            }

            @Override
            public Optional<RateLimitWindow> getActiveWindow(String key) {
                if (!postgresAvailable.get()) {
                    throw new CannotGetJdbcConnectionException("Simulated PostgreSQL primary connection failure");
                }
                return realPostgresStorage.getActiveWindow(key);
            }

            @Override
            public void remove(String key) {
                if (!postgresAvailable.get()) {
                    throw new CannotGetJdbcConnectionException("Simulated PostgreSQL primary connection failure");
                }
                realPostgresStorage.remove(key);
            }

            @Override
            public void reset() {
                if (postgresAvailable.get()) {
                    realPostgresStorage.reset();
                }
            }
        };

        ResilientRateLimitStorage resilientStorage = new ResilientRateLimitStorage(
                toggleablePrimary,
                fallbackMemoryStorage,
                auditLogger
        );
        RateLimitService resilientRateLimitService = new RateLimitService(
                resilientStorage,
                new InMemoryRateLimitStorage(),
                auditLogger,
                System::currentTimeMillis,
                20, 900_000L, 5, 900_000L, 10, 15, 10, 30, 20, 120, 30, 120, 100, 10_000L, 600_000L
        );
        AuthService resilientAuthService = new AuthService(
                userAccountRepository,
                passwordEncoder,
                jwtService,
                resilientRateLimitService,
                auditLogger
        );
        AuthController resilientAuthController = new AuthController(resilientAuthService, clientIpResolver);
        MockMvc mockMvcResilient = MockMvcBuilders.standaloneSetup(resilientAuthController)
                .setControllerAdvice(apiExceptionHandler)
                .build();

        // 1. Com PostgreSQL indisponível, enviar 5 falhas de login -> devem cair no fallback local e retornar 401
        for (int i = 1; i <= 5; i++) {
            mockMvcResilient.perform(post("/api/v1/auth/login")
                            .with(req -> {
                                req.setRemoteAddr("198.51.100.70");
                                return req;
                            })
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "email", TARGET_EMAIL,
                                    "password", WRONG_PASSWORD
                            ))))
                    .andExpect(status().isUnauthorized());
        }

        assertTrue(resilientStorage.isDegraded(), "O ResilientRateLimitStorage deve estar marcado como degradado.");

        // 2. A 6ª tentativa deve ser bloqueada com HTTP 429 pelo fallback em memória local
        mockMvcResilient.perform(post("/api/v1/auth/login")
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.70");
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", TARGET_EMAIL,
                                "password", VALID_PASSWORD
                        ))))
                .andExpect(status().isTooManyRequests());

        // 3. Confirmar registro do alerta operacional rate_limit_fallback
        SecurityAuditLogger.SecurityEvent fallbackAlert = auditLogger.getRecentEvents().stream()
                .filter(e -> "rate_limit_fallback".equals(e.eventType()))
                .findFirst()
                .orElse(null);
        assertNotNull(fallbackAlert, "O alerta operacional rate_limit_fallback deve ser emitido quando o PostgreSQL falha.");
        assertEquals("ALTA", fallbackAlert.severity());

        // 4. Restabelecer o PostgreSQL primário e realizar tentativa para confirmar recuperação automática
        postgresAvailable.set(true);
        mockMvcResilient.perform(post("/api/v1/auth/login")
                        .with(req -> {
                            req.setRemoteAddr("198.51.100.71");
                            return req;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "email", "recovered-account@borae.com",
                                "password", WRONG_PASSWORD
                        ))))
                .andExpect(status().isUnauthorized());

        assertFalse(resilientStorage.isDegraded(), "O armazenamento deve sair do estado degradado após o PostgreSQL responder.");
        assertTrue(auditLogger.getRecentEvents().stream()
                        .anyMatch(e -> "rate_limit_recovered".equals(e.eventType())),
                "O evento rate_limit_recovered deve ser registrado após o restabelecimento do PostgreSQL.");
        assertTrue(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM security_rate_limit WHERE bucket_key LIKE 'login-fail:%'",
                Integer.class
        ) >= 1, "Após a recuperação, novas falhas devem voltar a ser persistidas no PostgreSQL.");

        // 5. Confirmar que erro de configuração transacional (ex.: SQLState 25006 read_only_sql_transaction) NÃO é mascarado pelo fallback
        RateLimitStorage misconfiguredReadOnlyPrimary = new RateLimitStorage() {
            @Override
            public String name() {
                return "postgres";
            }

            @Override
            public boolean shared() {
                return true;
            }

            @Override
            public RateLimitWindow increment(String key, long windowMs) {
                throw new UncategorizedSQLException(
                        "increment",
                        "INSERT INTO security_rate_limit ...",
                        new SQLException("ERROR: cannot execute INSERT in a read-only transaction", "25006")
                );
            }

            @Override
            public void reset() {
            }
        };
        ResilientRateLimitStorage nonMaskingStorage = new ResilientRateLimitStorage(
                misconfiguredReadOnlyPrimary,
                new InMemoryRateLimitStorage(),
                auditLogger
        );
        assertThrows(UncategorizedSQLException.class,
                () -> nonMaskingStorage.increment("login-fail:test-readonly-check", 60_000L),
                "Erros de transação read-only (SQLState 25006) não podem ser mascarados pelo fallback em memória.");
    }
}
