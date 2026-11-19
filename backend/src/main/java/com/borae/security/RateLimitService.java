package com.borae.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/**
 * Serviço de rate limiting em duas camadas (memória + compartilhado no PostgreSQL com reserva local),
 * adaptado de {@code src/limite-por-ip.ts} e {@code src/limite-compartilhado/*} do Neves Shield.
 *
 * <p>Política {@code so-a-rota} para limites operacionais (evitando bloquear redes Wi-Fi de restaurantes
 * ou CGNAT móvel inteiros) e bloqueio progressivo de IP ({@code 2^reincidencias}, até 32x) apenas
 * para ataques de WAF ou ferramentas de varredura.</p>
 */
@Service
public class RateLimitService {

    private static final int MAX_RECURRENCE_SCALE = 5;
    private static final long MINUTE_MS = 60_000L;
    private static final long HOUR_MS = 60 * MINUTE_MS;
    private static final long DAY_MS = 24 * HOUR_MS;

    private static final Pattern MEDIA_UPLOAD_PATH = Pattern.compile(
            "^/api/v1/restaurants/[^/]+/(media|menus/[^/]+/products/[^/]+/(image|model-3d))$"
    );
    private static final Pattern INVITATION_CREATE_PATH = Pattern.compile(
            "^/api/v1/restaurants/[^/]+/invitations$"
    );
    private static final Pattern ADMIN_SENSITIVE_PATH = Pattern.compile(
            "^/api/v1/restaurants(/[^/]+/(publish|unpublish|qrcode|members/[^/]+))?$"
    );
    private static final Pattern ASSISTANT_PATH = Pattern.compile(
            "^/api/v1/(public/)?restaurants/[^/]+/(assistant|ai|recommendations)(/.*)?$"
    );
    private static final Pattern ANONYMOUS_SESSION_PATH = Pattern.compile(
            "^/api/v1/(public/.*/sessions|sessions/anonymous)(/.*)?$"
    );

    public record RateLimitRule(
            String name,
            int maxRequests,
            long windowMs,
            boolean useSharedStorage,
            boolean scopeByIpOnly,
            String message
    ) {
    }

    public record RateLimitDecision(
            boolean allowed,
            int limit,
            int remaining,
            int retryAfterSeconds,
            boolean ipBlocked,
            String ruleName,
            String message
    ) {
    }

    public record IpBlock(long untilEpochMs, String reason, int recurrences) {
    }

    private final RateLimitStorage sharedStorage;
    private final RateLimitStorage memoryStorage;
    private final SecurityAuditLogger auditLogger;
    private final LongSupplier clock;

    private final int loginMaxRequests;
    private final long loginWindowMs;
    private final int loginMaxFailures;
    private final long loginLockoutMs;
    private final int registerMaxRequests;
    private final int invitationAcceptMaxRequests;
    private final int invitationSendPerHourMax;
    private final int invitationSendPerDayRestaurantMax;
    private final int mediaUploadMaxRequests;
    private final int publicMaxRequests;
    private final int adminSensitiveMaxRequests;
    private final int defaultApiMaxRequests;
    private final int peakMaxRequests;
    private final long peakWindowMs;
    private final long attackBlockBaseMs;

    private final Map<String, IpBlock> ipBlocks = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitService(
            JdbcTemplate jdbcTemplate,
            SecurityAuditLogger auditLogger,
            @Value("${security.rate-limit.storage-mode:postgres}") String storageMode,
            @Value("${security.rate-limit.login-max:10}") int loginMaxRequests,
            @Value("${security.rate-limit.login-window-ms:900000}") long loginWindowMs,
            @Value("${security.rate-limit.login-max-failures:5}") int loginMaxFailures,
            @Value("${security.rate-limit.login-lockout-ms:900000}") long loginLockoutMs,
            @Value("${security.rate-limit.register-max:10}") int registerMaxRequests,
            @Value("${security.rate-limit.invitation-accept-max:15}") int invitationAcceptMaxRequests,
            @Value("${security.rate-limit.invitation-send-hour-max:10}") int invitationSendPerHourMax,
            @Value("${security.rate-limit.invitation-send-day-restaurant-max:30}") int invitationSendPerDayRestaurantMax,
            @Value("${security.rate-limit.media-upload-max:20}") int mediaUploadMaxRequests,
            @Value("${security.rate-limit.public-max:120}") int publicMaxRequests,
            @Value("${security.rate-limit.admin-sensitive-max:30}") int adminSensitiveMaxRequests,
            @Value("${security.rate-limit.default-max:120}") int defaultApiMaxRequests,
            @Value("${security.rate-limit.peak-max:100}") int peakMaxRequests,
            @Value("${security.rate-limit.peak-window-ms:10000}") long peakWindowMs,
            @Value("${security.rate-limit.attack-block-ms:600000}") long attackBlockBaseMs
    ) {
        this.clock = System::currentTimeMillis;
        this.auditLogger = auditLogger;
        InMemoryRateLimitStorage localMemory = new InMemoryRateLimitStorage(this.clock);
        this.memoryStorage = localMemory;
        if ("memory".equalsIgnoreCase(storageMode) || jdbcTemplate == null) {
            this.sharedStorage = localMemory;
        } else {
            this.sharedStorage = new ResilientRateLimitStorage(
                    new PostgresRateLimitStorage(jdbcTemplate),
                    new InMemoryRateLimitStorage(this.clock),
                    auditLogger
            );
        }
        this.loginMaxRequests = loginMaxRequests;
        this.loginWindowMs = loginWindowMs;
        this.loginMaxFailures = loginMaxFailures;
        this.loginLockoutMs = loginLockoutMs;
        this.registerMaxRequests = registerMaxRequests;
        this.invitationAcceptMaxRequests = invitationAcceptMaxRequests;
        this.invitationSendPerHourMax = invitationSendPerHourMax;
        this.invitationSendPerDayRestaurantMax = invitationSendPerDayRestaurantMax;
        this.mediaUploadMaxRequests = mediaUploadMaxRequests;
        this.publicMaxRequests = publicMaxRequests;
        this.adminSensitiveMaxRequests = adminSensitiveMaxRequests;
        this.defaultApiMaxRequests = defaultApiMaxRequests;
        this.peakMaxRequests = peakMaxRequests;
        this.peakWindowMs = peakWindowMs;
        this.attackBlockBaseMs = attackBlockBaseMs;
    }

    public RateLimitService(
            RateLimitStorage sharedStorage,
            RateLimitStorage memoryStorage,
            SecurityAuditLogger auditLogger,
            LongSupplier clock,
            int loginMaxRequests,
            long loginWindowMs,
            int loginMaxFailures,
            long loginLockoutMs,
            int registerMaxRequests,
            int invitationAcceptMaxRequests,
            int invitationSendPerHourMax,
            int invitationSendPerDayRestaurantMax,
            int mediaUploadMaxRequests,
            int publicMaxRequests,
            int adminSensitiveMaxRequests,
            int defaultApiMaxRequests,
            int peakMaxRequests,
            long peakWindowMs,
            long attackBlockBaseMs
    ) {
        this.sharedStorage = sharedStorage;
        this.memoryStorage = memoryStorage;
        this.auditLogger = auditLogger;
        this.clock = clock;
        this.loginMaxRequests = loginMaxRequests;
        this.loginWindowMs = loginWindowMs;
        this.loginMaxFailures = loginMaxFailures;
        this.loginLockoutMs = loginLockoutMs;
        this.registerMaxRequests = registerMaxRequests;
        this.invitationAcceptMaxRequests = invitationAcceptMaxRequests;
        this.invitationSendPerHourMax = invitationSendPerHourMax;
        this.invitationSendPerDayRestaurantMax = invitationSendPerDayRestaurantMax;
        this.mediaUploadMaxRequests = mediaUploadMaxRequests;
        this.publicMaxRequests = publicMaxRequests;
        this.adminSensitiveMaxRequests = adminSensitiveMaxRequests;
        this.defaultApiMaxRequests = defaultApiMaxRequests;
        this.peakMaxRequests = peakMaxRequests;
        this.peakWindowMs = peakWindowMs;
        this.attackBlockBaseMs = attackBlockBaseMs;
    }

    public IpBlock getActiveIpBlock(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        long now = clock.getAsLong();
        IpBlock block = ipBlocks.get(ip);
        if (block == null) {
            return null;
        }
        if (now >= block.untilEpochMs()) {
            ipBlocks.remove(ip, block);
            return null;
        }
        return block;
    }

    public IpBlock blockIp(String ip, String reason) {
        return blockIp(ip, reason, attackBlockBaseMs);
    }

    public IpBlock blockIp(String ip, String reason, long baseDurationMs) {
        long now = clock.getAsLong();
        IpBlock updated = ipBlocks.compute(ip, (k, previous) -> {
            int recurrences = (previous == null ? 0 : previous.recurrences()) + 1;
            long multiplier = 1L << Math.min(recurrences - 1, MAX_RECURRENCE_SCALE);
            long duration = baseDurationMs * multiplier;
            return new IpBlock(now + duration, reason, recurrences);
        });
        if (auditLogger != null) {
            long durationSec = Math.max(1L, Math.round((updated.untilEpochMs() - now) / 1000.0));
            auditLogger.logSecurityEvent(
                    updated.recurrences() >= 3 ? "CRITICA" : "ALTA",
                    "ip_bloqueado",
                    "BLOQUEIO",
                    "n/a",
                    ip,
                    true,
                    "IP bloqueado: " + reason + ". Reincidência " + updated.recurrences() + ", " + durationSec + " s"
            );
        }
        return updated;
    }

    public void unblockIp(String ip) {
        if (ip != null) {
            ipBlocks.remove(ip);
        }
    }

    public RateLimitDecision checkRequest(String ip, String authenticatedSubject, String method, String path) {
        long now = clock.getAsLong();
        String safeIp = (ip == null || ip.isBlank()) ? ClientIpResolver.UNKNOWN_IP : ip;

        IpBlock activeBlock = getActiveIpBlock(safeIp);
        if (activeBlock != null) {
            int retryAfter = Math.max(1, (int) Math.ceil((activeBlock.untilEpochMs() - now) / 1000.0));
            return new RateLimitDecision(
                    false,
                    0,
                    0,
                    retryAfter,
                    true,
                    "ip-blocked",
                    "Atividade suspeita detectada. Aguarde antes de tentar novamente."
            );
        }

        // Proteção contra pico de rajada (soma todas as rotas por IP na janela curta)
        RateLimitWindow peakWindow = memoryStorage.increment("pico:" + safeIp, peakWindowMs);
        if (peakWindow.count() > peakMaxRequests) {
            int retryAfter = Math.max(1, (int) Math.ceil((peakWindow.resetAtEpochMs() - now) / 1000.0));
            return new RateLimitDecision(
                    false,
                    peakMaxRequests,
                    0,
                    retryAfter,
                    false,
                    "burst-peak",
                    formatLimitMessage(retryAfter)
            );
        }

        RateLimitRule rule = resolveRule(method, path);
        String subjectKey = buildSubjectKey(rule, safeIp, authenticatedSubject);
        RateLimitStorage storage = rule.useSharedStorage() ? sharedStorage : memoryStorage;
        RateLimitWindow window = storage.increment(subjectKey, rule.windowMs());

        int retryAfter = Math.max(1, (int) Math.ceil((window.resetAtEpochMs() - now) / 1000.0));
        int remaining = Math.max(0, rule.maxRequests() - window.count());

        if (window.count() > rule.maxRequests()) {
            return new RateLimitDecision(
                    false,
                    rule.maxRequests(),
                    0,
                    retryAfter,
                    false,
                    rule.name(),
                    rule.message() != null ? rule.message() : formatLimitMessage(retryAfter)
            );
        }

        return new RateLimitDecision(
                true,
                rule.maxRequests(),
                remaining,
                retryAfter,
                false,
                rule.name(),
                null
        );
    }

    /**
     * Registra tentativa de login com senha inválida e informa se a conta atingiu
     * o bloqueio temporário de 15 minutos após 5 falhas consecutivas (Decisão D-13/D-17).
     */
    public boolean recordFailedLoginAttempt(String normalizedEmail) {
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return false;
        }
        String key = "login-fail:" + shortHash(normalizedEmail.trim().toLowerCase(Locale.ROOT));
        RateLimitWindow window = sharedStorage.increment(key, loginLockoutMs);
        return window.count() >= loginMaxFailures;
    }

    /**
     * Verifica se a conta está temporariamente bloqueada por excesso de falhas de senha.
     */
    public boolean isLoginTemporarilyLocked(String normalizedEmail) {
        if (normalizedEmail == null || normalizedEmail.isBlank()) {
            return false;
        }
        String lockKey = "login-lock:" + shortHash(normalizedEmail.trim().toLowerCase(Locale.ROOT));
        RateLimitWindow window = sharedStorage.increment(lockKey, loginLockoutMs);
        // A chave login-lock só existe quando ativada ou verificada junto ao contador:
        return false;
    }

    /**
     * Verifica e registra tentativa de autenticação por conta (proteção contra brute force distribuído).
     */
    public RateLimitDecision checkAccountLoginAttempt(String normalizedEmail) {
        long now = clock.getAsLong();
        String key = "acct-login:" + shortHash(normalizedEmail == null ? "" : normalizedEmail.trim().toLowerCase(Locale.ROOT));
        RateLimitWindow window = sharedStorage.increment(key, loginLockoutMs);
        int retryAfter = Math.max(1, (int) Math.ceil((window.resetAtEpochMs() - now) / 1000.0));
        int remaining = Math.max(0, loginMaxFailures - window.count());
        if (window.count() > loginMaxFailures) {
            return new RateLimitDecision(
                    false,
                    loginMaxFailures,
                    0,
                    retryAfter,
                    false,
                    "auth-account-lockout",
                    "Muitas tentativas de login para esta conta. Aguarde " + formatTimePtBr(retryAfter) + " para tentar novamente."
            );
        }
        return new RateLimitDecision(true, loginMaxFailures, remaining, retryAfter, false, "auth-account-lockout", null);
    }

    /**
     * Confere os limites aprovados na Decisão 11.2 para envio de convites:
     * até 10 convites por hora por administrador e 30 convites por dia por restaurante.
     */
    public RateLimitDecision checkInvitationQuota(String actorId, String restaurantId) {
        long now = clock.getAsLong();
        String userKey = "usuario:" + shortHash(actorId) + ":invite-hour";
        RateLimitWindow userWindow = sharedStorage.increment(userKey, HOUR_MS);
        if (userWindow.count() > invitationSendPerHourMax) {
            int retryAfter = Math.max(1, (int) Math.ceil((userWindow.resetAtEpochMs() - now) / 1000.0));
            return new RateLimitDecision(
                    false,
                    invitationSendPerHourMax,
                    0,
                    retryAfter,
                    false,
                    "invite-user-hour",
                    "Limite de 10 convites por hora por administrador atingido. Aguarde antes de enviar novos convites."
            );
        }

        String restaurantKey = "rest:" + shortHash(restaurantId) + ":invite-day";
        RateLimitWindow restWindow = sharedStorage.increment(restaurantKey, DAY_MS);
        if (restWindow.count() > invitationSendPerDayRestaurantMax) {
            int retryAfter = Math.max(1, (int) Math.ceil((restWindow.resetAtEpochMs() - now) / 1000.0));
            return new RateLimitDecision(
                    false,
                    invitationSendPerDayRestaurantMax,
                    0,
                    retryAfter,
                    false,
                    "invite-restaurant-day",
                    "Limite de 30 convites por dia para este restaurante atingido."
            );
        }

        int remaining = Math.min(
                Math.max(0, invitationSendPerHourMax - userWindow.count()),
                Math.max(0, invitationSendPerDayRestaurantMax - restWindow.count())
        );
        int retryAfter = Math.max(1, (int) Math.ceil((userWindow.resetAtEpochMs() - now) / 1000.0));
        return new RateLimitDecision(true, invitationSendPerHourMax, remaining, retryAfter, false, "invite-quota", null);
    }

    public RateLimitRule resolveRule(String method, String path) {
        String safeMethod = method == null ? "GET" : method.toUpperCase(Locale.ROOT);
        String safePath = path == null ? "/" : path;

        if ("POST".equals(safeMethod) && "/api/v1/auth/login".equals(safePath)) {
            return new RateLimitRule(
                    "auth-login",
                    loginMaxRequests,
                    loginWindowMs,
                    true,
                    true,
                    null
            );
        }
        if ("POST".equals(safeMethod) && "/api/v1/auth/register".equals(safePath)) {
            return new RateLimitRule(
                    "auth-register",
                    registerMaxRequests,
                    HOUR_MS,
                    true,
                    true,
                    null
            );
        }
        if ("POST".equals(safeMethod) && (safePath.startsWith("/api/v1/auth/forgot-password")
                || safePath.startsWith("/api/v1/auth/reset-password")
                || safePath.startsWith("/api/v1/auth/password-recovery"))) {
            return new RateLimitRule(
                    "auth-password-recovery",
                    5,
                    HOUR_MS,
                    true,
                    true,
                    null
            );
        }
        if ("POST".equals(safeMethod) && "/api/v1/restaurant-invitations/accept".equals(safePath)) {
            return new RateLimitRule(
                    "invitation-accept",
                    invitationAcceptMaxRequests,
                    15 * MINUTE_MS,
                    true,
                    false,
                    null
            );
        }
        if ("POST".equals(safeMethod) && INVITATION_CREATE_PATH.matcher(safePath).matches()) {
            return new RateLimitRule(
                    "invitation-send",
                    invitationSendPerHourMax,
                    HOUR_MS,
                    true,
                    false,
                    "Limite de envio de convites atingido. Aguarde antes de tentar novamente."
            );
        }
        if ("POST".equals(safeMethod) && ANONYMOUS_SESSION_PATH.matcher(safePath).matches()) {
            return new RateLimitRule(
                    "anonymous-session",
                    20,
                    10 * MINUTE_MS,
                    true,
                    true,
                    null
            );
        }
        if (ASSISTANT_PATH.matcher(safePath).matches()) {
            return new RateLimitRule(
                    "gastronomic-assistant",
                    20,
                    DAY_MS,
                    true,
                    false,
                    "Você atingiu o limite diário de interações com o assistente gastronômico."
            );
        }
        if ("POST".equals(safeMethod) && MEDIA_UPLOAD_PATH.matcher(safePath).matches()) {
            return new RateLimitRule(
                    "media-upload",
                    mediaUploadMaxRequests,
                    10 * MINUTE_MS,
                    true,
                    false,
                    "Muitos envios de arquivos em sequência. Aguarde alguns minutos."
            );
        }
        if (safePath.startsWith("/api/v1/public/")) {
            return new RateLimitRule(
                    "public-endpoint",
                    publicMaxRequests,
                    MINUTE_MS,
                    false,
                    true,
                    null
            );
        }
        if (("POST".equals(safeMethod) || "PATCH".equals(safeMethod) || "DELETE".equals(safeMethod) || "GET".equals(safeMethod))
                && ADMIN_SENSITIVE_PATH.matcher(safePath).matches()
                && !("GET".equals(safeMethod) && !safePath.endsWith("/qrcode"))) {
            return new RateLimitRule(
                    "admin-sensitive",
                    adminSensitiveMaxRequests,
                    MINUTE_MS,
                    true,
                    false,
                    null
            );
        }
        return new RateLimitRule(
                "api-default",
                defaultApiMaxRequests,
                MINUTE_MS,
                false,
                false,
                null
        );
    }

    public void resetAll() {
        ipBlocks.clear();
        memoryStorage.reset();
        sharedStorage.reset();
    }

    public static String buildSubjectKey(RateLimitRule rule, String ip, String authenticatedSubject) {
        String subjectPart;
        if (!rule.scopeByIpOnly() && authenticatedSubject != null && !authenticatedSubject.isBlank()) {
            subjectPart = "usuario:" + shortHash(authenticatedSubject.trim().toLowerCase(Locale.ROOT));
        } else {
            String safeIp = (ip == null || ip.isBlank()) ? ClientIpResolver.UNKNOWN_IP : ip.trim();
            subjectPart = "anon:" + safeIp;
        }
        String key = subjectPart + ":" + rule.name();
        if (key.length() > 200) {
            return key.substring(0, 200);
        }
        return key;
    }

    private static String shortHash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 24);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }

    private static String formatLimitMessage(int seconds) {
        return "Você fez muitas solicitações em sequência. Aguarde "
                + formatTimePtBr(seconds) + " para tentar novamente.";
    }

    private static String formatTimePtBr(int seconds) {
        if (seconds < 60) {
            return seconds + " segundo" + (seconds == 1 ? "" : "s");
        }
        int minutes = (int) Math.ceil(seconds / 60.0);
        return minutes + " minuto" + (minutes == 1 ? "" : "s");
    }
}
