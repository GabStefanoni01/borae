package com.borae.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

/**
 * Resolução segura do IP do cliente com validação explícita da cadeia de proxies confiáveis.
 *
 * <p>Somente interpreta cabeçalhos de encaminhamento ({@code X-Forwarded-For}, {@code X-Real-IP},
 * {@code CF-Connecting-IP}) quando {@code request.getRemoteAddr()} pertence a um proxy ou rede CIDR
 * explicitamente configurado em {@code security.trusted-proxies}. Em {@code X-Forwarded-For}, percorre
 * a cadeia da direita para a esquerda e retorna o primeiro endereço que não pertence à lista de proxies
 * confiáveis, impedindo spoofing pelo cliente na extremidade esquerda do cabeçalho.</p>
 */
@Component
public class ClientIpResolver {

    public static final String UNKNOWN_IP = "desconhecido";
    private static final int MAX_HEADER_LENGTH = 512;
    private static final int MAX_CHAIN_HOPS = 16;
    private static final int MAX_IP_LITERAL_LENGTH = 45;

    private final List<String> trustedHeaders;
    private final List<CidrRule> trustedProxies;
    private final SecurityAuditLogger auditLogger;

    public ClientIpResolver(String configuredHeaders, boolean trustLoopbackForTests) {
        this(configuredHeaders, trustLoopbackForTests ? "127.0.0.1/32,::1/128" : "", null);
    }

    public ClientIpResolver(String configuredHeaders, String trustedProxiesConfig) {
        this(configuredHeaders, trustedProxiesConfig, null);
    }

    @Autowired
    public ClientIpResolver(
            @Value("${security.ip-headers:X-Forwarded-For,X-Real-IP,CF-Connecting-IP}") String configuredHeaders,
            @Value("${security.trusted-proxies:}") String trustedProxiesConfig,
            SecurityAuditLogger auditLogger
    ) {
        this.trustedHeaders = Arrays.stream((configuredHeaders == null ? "" : configuredHeaders).split(","))
                .map(String::trim)
                .filter(header -> !header.isBlank())
                .toList();
        this.trustedProxies = parseTrustedProxies(trustedProxiesConfig);
        this.auditLogger = auditLogger;
    }

    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN_IP;
        }
        String remoteIp = normalizeIpLiteral(request.getRemoteAddr());
        if (remoteIp == null) {
            return UNKNOWN_IP;
        }

        if (!isTrustedProxy(remoteIp)) {
            if (hasAnyForwardedHeader(request) && auditLogger != null) {
                auditLogger.logSecurityEvent(
                        "BAIXA",
                        "untrusted_forwarded_header",
                        request.getMethod(),
                        request.getRequestURI(),
                        remoteIp,
                        false,
                        "Cabeçalho de IP encaminhado ignorado: conexão direta de origem não listada em trusted-proxies."
                );
            }
            return remoteIp;
        }

        for (String headerName : trustedHeaders) {
            List<String> headerValues = extractHeaderValues(request, headerName);
            if (headerValues.isEmpty()) {
                continue;
            }
            String combined = String.join(",", headerValues).trim();
            if (combined.isEmpty()) {
                continue;
            }
            if (combined.length() > MAX_HEADER_LENGTH) {
                logMalformedHeader(request, remoteIp, headerName, "Cabeçalho de IP excede o tamanho máximo permitido.");
                return remoteIp;
            }

            if ("X-Forwarded-For".equalsIgnoreCase(headerName)) {
                String resolved = resolveFromForwardedChain(combined, request, remoteIp);
                if (resolved != null) {
                    return resolved;
                }
                return remoteIp;
            } else {
                if (combined.contains(",")) {
                    logMalformedHeader(request, remoteIp, headerName, "Cabeçalho de IP único contém múltiplos valores.");
                    return remoteIp;
                }
                String singleIp = normalizeIpLiteral(combined);
                if (singleIp == null) {
                    logMalformedHeader(request, remoteIp, headerName, "Endereço IP malformado no cabeçalho.");
                    return remoteIp;
                }
                return singleIp;
            }
        }

        return remoteIp;
    }

    public boolean isTrustedProxy(String ipAddress) {
        if (trustedProxies.isEmpty() || ipAddress == null || ipAddress.isBlank()) {
            return false;
        }
        byte[] addressBytes = parseIpLiteralToBytes(ipAddress);
        if (addressBytes == null) {
            return false;
        }
        for (CidrRule rule : trustedProxies) {
            if (rule.matches(addressBytes)) {
                return true;
            }
        }
        return false;
    }

    private String resolveFromForwardedChain(String rawChain, HttpServletRequest request, String remoteIp) {
        String[] parts = rawChain.split(",", -1);
        if (parts.length == 0 || parts.length > MAX_CHAIN_HOPS) {
            logMalformedHeader(request, remoteIp, "X-Forwarded-For", "Cadeia X-Forwarded-For com número inválido de saltos.");
            return null;
        }

        List<String> normalizedHops = new ArrayList<>(parts.length);
        for (String part : parts) {
            String normalized = normalizeIpLiteral(part);
            if (normalized == null) {
                logMalformedHeader(request, remoteIp, "X-Forwarded-For", "Endereço inválido na cadeia X-Forwarded-For.");
                return null;
            }
            normalizedHops.add(normalized);
        }

        // Travessia da direita para a esquerda: o último salto não confiável é o cliente real.
        for (int i = normalizedHops.size() - 1; i >= 0; i--) {
            String hop = normalizedHops.get(i);
            if (!isTrustedProxy(hop)) {
                return hop;
            }
        }
        return normalizedHops.get(0);
    }

    private List<String> extractHeaderValues(HttpServletRequest request, String headerName) {
        Enumeration<String> headers = request.getHeaders(headerName);
        if (headers == null) {
            String single = request.getHeader(headerName);
            return single == null ? List.of() : List.of(single);
        }
        List<String> values = Collections.list(headers);
        if (values.isEmpty()) {
            String single = request.getHeader(headerName);
            if (single != null) {
                return List.of(single);
            }
        }
        return values;
    }

    private boolean hasAnyForwardedHeader(HttpServletRequest request) {
        for (String headerName : trustedHeaders) {
            String val = request.getHeader(headerName);
            if (val != null && !val.isBlank()) {
                return true;
            }
        }
        return false;
    }

    private void logMalformedHeader(HttpServletRequest request, String remoteIp, String headerName, String detail) {
        if (auditLogger != null) {
            auditLogger.logSecurityEvent(
                    "MEDIA",
                    "malformed_ip_header",
                    request.getMethod(),
                    request.getRequestURI(),
                    remoteIp,
                    false,
                    headerName + ": " + detail
            );
        }
    }

    public static String normalizeIpLiteral(String raw) {
        byte[] bytes = parseIpLiteralToBytes(raw);
        if (bytes == null) {
            return null;
        }
        try {
            return InetAddress.getByAddress(bytes).getHostAddress();
        } catch (UnknownHostException exception) {
            return null;
        }
    }

    /**
     * Converte um literal IPv4 ou IPv6 em bytes sem jamais realizar resolução DNS.
     */
    public static byte[] parseIpLiteralToBytes(String raw) {
        if (raw == null) {
            return null;
        }
        String candidate = raw.trim();
        if (candidate.isEmpty()) {
            return null;
        }
        if (candidate.startsWith("[") && candidate.endsWith("]") && candidate.length() > 2) {
            candidate = candidate.substring(1, candidate.length() - 1).trim();
        }
        if (candidate.isEmpty() || candidate.length() > MAX_IP_LITERAL_LENGTH) {
            return null;
        }
        for (int i = 0; i < candidate.length(); i++) {
            char c = candidate.charAt(i);
            boolean validChar = (c >= '0' && c <= '9')
                    || (c >= 'a' && c <= 'f')
                    || (c >= 'A' && c <= 'F')
                    || c == '.'
                    || c == ':';
            if (!validChar) {
                return null;
            }
        }
        if (candidate.contains(":")) {
            return parseIpv6Literal(candidate);
        }
        return parseIpv4Literal(candidate);
    }

    private static byte[] parseIpv4Literal(String input) {
        String[] parts = input.split("\\.", -1);
        if (parts.length != 4) {
            return null;
        }
        byte[] result = new byte[4];
        for (int i = 0; i < 4; i++) {
            String part = parts[i];
            if (part.isEmpty() || part.length() > 3) {
                return null;
            }
            if (part.length() > 1 && part.charAt(0) == '0') {
                return null;
            }
            for (int j = 0; j < part.length(); j++) {
                if (!Character.isDigit(part.charAt(j))) {
                    return null;
                }
            }
            int octet = Integer.parseInt(part);
            if (octet < 0 || octet > 255) {
                return null;
            }
            result[i] = (byte) octet;
        }
        return result;
    }

    private static byte[] parseIpv6Literal(String input) {
        String working = input;
        byte[] embeddedIpv4 = null;
        if (working.contains(".")) {
            int lastColon = working.lastIndexOf(':');
            if (lastColon < 0) {
                return null;
            }
            embeddedIpv4 = parseIpv4Literal(working.substring(lastColon + 1));
            if (embeddedIpv4 == null) {
                return null;
            }
            int high = ((embeddedIpv4[0] & 0xFF) << 8) | (embeddedIpv4[1] & 0xFF);
            int low = ((embeddedIpv4[2] & 0xFF) << 8) | (embeddedIpv4[3] & 0xFF);
            working = working.substring(0, lastColon + 1)
                    + Integer.toHexString(high) + ":" + Integer.toHexString(low);
        }

        int doubleColonIndex = working.indexOf("::");
        if (doubleColonIndex >= 0 && working.indexOf("::", doubleColonIndex + 1) >= 0) {
            return null;
        }

        String[] leftParts;
        String[] rightParts;
        if (doubleColonIndex >= 0) {
            String left = working.substring(0, doubleColonIndex);
            String right = working.substring(doubleColonIndex + 2);
            if (left.startsWith(":") || right.endsWith(":")) {
                return null;
            }
            leftParts = left.isEmpty() ? new String[0] : left.split(":", -1);
            rightParts = right.isEmpty() ? new String[0] : right.split(":", -1);
            if (leftParts.length + rightParts.length >= 8) {
                return null;
            }
        } else {
            leftParts = working.split(":", -1);
            rightParts = new String[0];
            if (leftParts.length != 8) {
                return null;
            }
        }

        byte[] bytes = new byte[16];
        int groupIndex = 0;
        for (String group : leftParts) {
            int val = parseHexGroup(group);
            if (val < 0) {
                return null;
            }
            bytes[groupIndex * 2] = (byte) ((val >> 8) & 0xFF);
            bytes[groupIndex * 2 + 1] = (byte) (val & 0xFF);
            groupIndex++;
        }

        int rightStart = 8 - rightParts.length;
        for (int i = 0; i < rightParts.length; i++) {
            int val = parseHexGroup(rightParts[i]);
            if (val < 0) {
                return null;
            }
            int idx = (rightStart + i) * 2;
            bytes[idx] = (byte) ((val >> 8) & 0xFF);
            bytes[idx + 1] = (byte) (val & 0xFF);
        }
        return bytes;
    }

    private static int parseHexGroup(String group) {
        if (group == null || group.isEmpty() || group.length() > 4) {
            return -1;
        }
        int value = 0;
        for (int i = 0; i < group.length(); i++) {
            int digit = Character.digit(group.charAt(i), 16);
            if (digit < 0) {
                return -1;
            }
            value = (value << 4) | digit;
        }
        return value;
    }

    private static List<CidrRule> parseTrustedProxies(String config) {
        if (config == null || config.isBlank()) {
            return List.of();
        }
        List<CidrRule> rules = new ArrayList<>();
        for (String rawEntry : config.split(",")) {
            String entry = rawEntry.trim();
            if (entry.isEmpty()) {
                continue;
            }
            rules.add(CidrRule.parse(entry));
        }
        return List.copyOf(rules);
    }

    private record CidrRule(byte[] networkBytes, int prefixBits) {

        static CidrRule parse(String cidrOrIp) {
            String trimmed = cidrOrIp.trim();
            int slashIndex = trimmed.indexOf('/');
            String ipPart = slashIndex >= 0 ? trimmed.substring(0, slashIndex).trim() : trimmed;
            byte[] bytes = parseIpLiteralToBytes(ipPart);
            if (bytes == null) {
                throw new IllegalStateException("Endereço ou CIDR de proxy confiável inválido: " + cidrOrIp);
            }
            int maxBits = bytes.length * 8;
            int prefix = maxBits;
            if (slashIndex >= 0) {
                String prefixPart = trimmed.substring(slashIndex + 1).trim();
                try {
                    prefix = Integer.parseInt(prefixPart);
                } catch (NumberFormatException exception) {
                    throw new IllegalStateException("Prefixo CIDR inválido em trusted-proxies: " + cidrOrIp);
                }
                if (prefix < 0 || prefix > maxBits) {
                    throw new IllegalStateException("Prefixo CIDR fora do intervalo permitido: " + cidrOrIp);
                }
            }
            return new CidrRule(bytes, prefix);
        }

        boolean matches(byte[] candidate) {
            byte[] normalizedCandidate = normalizeIpv4Mapped(candidate);
            byte[] normalizedNetwork = normalizeIpv4Mapped(networkBytes);
            if (normalizedCandidate.length != normalizedNetwork.length) {
                return false;
            }
            int fullBytes = prefixBits / 8;
            int remainingBits = prefixBits % 8;
            for (int i = 0; i < fullBytes; i++) {
                if (normalizedCandidate[i] != normalizedNetwork[i]) {
                    return false;
                }
            }
            if (remainingBits > 0) {
                int mask = (0xFF << (8 - remainingBits)) & 0xFF;
                if ((normalizedCandidate[fullBytes] & mask) != (normalizedNetwork[fullBytes] & mask)) {
                    return false;
                }
            }
            return true;
        }

        private static byte[] normalizeIpv4Mapped(byte[] address) {
            if (address.length == 16) {
                boolean isMapped = true;
                for (int i = 0; i < 10; i++) {
                    if (address[i] != 0) {
                        isMapped = false;
                        break;
                    }
                }
                if (isMapped && (address[10] & 0xFF) == 0xFF && (address[11] & 0xFF) == 0xFF) {
                    return new byte[]{address[12], address[13], address[14], address[15]};
                }
            }
            return address;
        }
    }
}
