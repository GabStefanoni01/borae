package com.borae.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SecurityAuditLogger auditLogger;
    private final ClientIpResolver ipResolver;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this(jwtService, null, null);
    }

    public JwtAuthenticationFilter(
            JwtService jwtService,
            SecurityAuditLogger auditLogger,
            ClientIpResolver ipResolver
    ) {
        this.jwtService = jwtService;
        this.auditLogger = auditLogger;
        this.ipResolver = ipResolver;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring(7).trim();
            try {
                String email = jwtService.extractSubject(token);
                var authentication = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (ExpiredJwtException exception) {
                SecurityContextHolder.clearContext();
                logJwtFailure(request, "jwt_expired", "Token JWT expirado.");
            } catch (JwtException | IllegalArgumentException exception) {
                SecurityContextHolder.clearContext();
                logJwtFailure(request, "jwt_invalid", "Token JWT inválido ou com assinatura incorreta.");
            }
        }

        filterChain.doFilter(request, response);
    }

    private void logJwtFailure(HttpServletRequest request, String eventType, String detail) {
        if (auditLogger == null) {
            return;
        }
        String clientIp = ipResolver != null ? ipResolver.resolve(request) : request.getRemoteAddr();
        auditLogger.logSecurityEvent(
                "MEDIA",
                eventType,
                request.getMethod(),
                request.getRequestURI(),
                clientIp,
                true,
                detail
        );
    }
}
