package com.borae.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Resolução segura do IP do cliente, adaptada de {@code src/ip.ts} do Neves Shield.
 *
 * <p>Em hospedagem Render/Cloudflare, utiliza apenas os cabeçalhos confiáveis configurados
 * para o ambiente, descartando valores malformados e fazendo fallback para o endereço do socket.</p>
 */
@Component
public class ClientIpResolver {

    public static final String UNKNOWN_IP = "desconhecido";
    private static final Pattern SAFE_IP_PATTERN = Pattern.compile("^[0-9a-fA-F:.]{3,64}$");

    private final List<String> trustedHeaders;
    private final boolean trustForwardedHeaders;

    public ClientIpResolver(
            @Value("${security.ip-headers:X-Forwarded-For,X-Real-IP,CF-Connecting-IP}") String configuredHeaders,
            @Value("${security.trust-forwarded-headers:true}") boolean trustForwardedHeaders
    ) {
        this.trustedHeaders = Arrays.stream((configuredHeaders == null ? "" : configuredHeaders).split(","))
                .map(String::trim)
                .filter(header -> !header.isBlank())
                .toList();
        this.trustForwardedHeaders = trustForwardedHeaders;
    }

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN_IP;
        }
        if (trustForwardedHeaders) {
            for (String headerName : trustedHeaders) {
                String rawValue = request.getHeader(headerName);
                if (rawValue == null || rawValue.isBlank()) {
                    continue;
                }
                String firstHop = rawValue.split(",")[0].trim();
                if (!firstHop.isEmpty() && SAFE_IP_PATTERN.matcher(firstHop).matches()) {
                    return firstHop;
                }
            }
        }
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr != null && !remoteAddr.isBlank() && SAFE_IP_PATTERN.matcher(remoteAddr.trim()).matches()) {
            return remoteAddr.trim();
        }
        return UNKNOWN_IP;
    }
}
