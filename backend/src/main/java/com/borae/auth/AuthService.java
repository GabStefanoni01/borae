package com.borae.auth;

import com.borae.auth.dto.AuthResponse;
import com.borae.auth.dto.LoginRequest;
import com.borae.auth.dto.RegisterRequest;
import com.borae.identity.UserAccount;
import com.borae.identity.UserAccountRepository;
import com.borae.security.ClientIpResolver;
import com.borae.security.JwtService;
import com.borae.security.RateLimitService;
import com.borae.security.SecurityAuditLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private static final String DUMMY_BCRYPT_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOHi6M4.v2S/p4uL2vK9fG6W9xY5L1r2e";

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RateLimitService rateLimitService;
    private final SecurityAuditLogger auditLogger;

    public AuthService(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this(userRepository, passwordEncoder, jwtService, null, null);
    }

    public AuthService(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            SecurityAuditLogger auditLogger
    ) {
        this(userRepository, passwordEncoder, jwtService, null, auditLogger);
    }

    @Autowired
    public AuthService(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RateLimitService rateLimitService,
            SecurityAuditLogger auditLogger
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.rateLimitService = rateLimitService;
        this.auditLogger = auditLogger;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado.");
        }

        UserAccount user = new UserAccount(
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password())
        );
        userRepository.save(user);
        return createResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        return login(request, ClientIpResolver.UNKNOWN_IP);
    }

    public AuthResponse login(LoginRequest request, String clientIp) {
        String email = normalizeEmail(request.email());
        String safeIp = (clientIp == null || clientIp.isBlank()) ? ClientIpResolver.UNKNOWN_IP : clientIp;

        // 1. Verificar se a conta/origem já atingiu o bloqueio temporário de 5 falhas consecutivas
        if (rateLimitService != null) {
            rateLimitService.assertLoginAttemptAllowed(email, safeIp);
        }

        Optional<UserAccount> candidate = userRepository.findByEmail(email);

        if (candidate.isEmpty()) {
            // Mitigação contra enumeração de contas: mesmo custo BCrypt e mesma contagem de falhas
            passwordEncoder.matches(request.password(), DUMMY_BCRYPT_HASH);
            if (rateLimitService != null) {
                rateLimitService.recordLoginFailure(email, safeIp);
            }
            logFailedLogin(safeIp, "Tentativa de login com credenciais inválidas.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        }

        UserAccount user = candidate.get();
        boolean passwordMatches = passwordEncoder.matches(request.password(), user.getPasswordHash());
        if (!user.isActive() || !passwordMatches) {
            if (rateLimitService != null) {
                rateLimitService.recordLoginFailure(email, safeIp);
            }
            logFailedLogin(safeIp, "Tentativa de login com credenciais inválidas ou conta inativa.");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        }

        if (rateLimitService != null) {
            rateLimitService.clearLoginFailures(email, safeIp);
        }

        return createResponse(user);
    }

    private void logFailedLogin(String clientIp, String detail) {
        if (auditLogger != null) {
            auditLogger.logSecurityEvent(
                    "MEDIA",
                    "login_failed",
                    "POST",
                    "/api/v1/auth/login",
                    clientIp,
                    true,
                    detail
            );
        }
    }

    private AuthResponse createResponse(UserAccount user) {
        return new AuthResponse(
                jwtService.generateToken(user.getEmail()),
                "Bearer",
                jwtService.getExpirationMs() / 1000,
                new AuthResponse.UserSummary(user.getId(), user.getFullName(), user.getEmail())
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
