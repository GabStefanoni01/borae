package com.borae.auth;

import com.borae.auth.dto.AuthResponse;
import com.borae.auth.dto.LoginRequest;
import com.borae.auth.dto.RegisterRequest;
import com.borae.security.ClientIpResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Autenticação", description = "Cadastro e autenticação de usuários")
public class AuthController {

    private final AuthService authService;
    private final ClientIpResolver ipResolver;

    public AuthController(AuthService authService) {
        this(authService, null);
    }

    @Autowired
    public AuthController(AuthService authService, ClientIpResolver ipResolver) {
        this.authService = authService;
        this.ipResolver = ipResolver;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Criar conta", description = "Cadastra um usuário e retorna um access token JWT.")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Entrar", description = "Autentica um usuário e retorna um access token JWT.")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String clientIp = ipResolver != null ? ipResolver.resolve(servletRequest) : ClientIpResolver.UNKNOWN_IP;
        return authService.login(request, clientIp);
    }
}
