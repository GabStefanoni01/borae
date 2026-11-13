package com.borae.auth;

import com.borae.auth.dto.AuthResponse;
import com.borae.auth.dto.LoginRequest;
import com.borae.auth.dto.RegisterRequest;
import com.borae.identity.UserAccount;
import com.borae.identity.UserAccountRepository;
import com.borae.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserAccountRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
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

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserAccount user = userRepository.findByEmail(email)
                .filter(UserAccount::isActive)
                .filter(account -> passwordEncoder.matches(request.password(), account.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "E-mail ou senha inválidos."
                ));

        return createResponse(user);
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
