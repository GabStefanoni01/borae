package com.borae.security;

import org.springframework.stereotype.Component;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inspetor WAF de requisições HTTP, adaptado de {@code src/waf.ts} do Neves Shield.
 *
 * <p>Utiliza expressões regulares com repetição limitada (protegidas contra ReDoS) e
 * normalização de codificações ({@code formasDeInspecao}) para identificar ferramentas
 * de varredura, SQL Injection, XSS, Command Injection, Path Traversal e Header Injection.</p>
 */
@Component
public class WafRequestInspector {

    public record Threat(String type, String severity, String detail) {
        public boolean isHighOrCritical() {
            return "alta".equals(severity) || "critica".equals(severity);
        }
    }

    private record PatternGroup(String type, String severity, List<Pattern> patterns) {
    }

    private static final List<Pattern> SCANNER_USER_AGENTS = List.of(
            Pattern.compile("(?i)sqlmap"),
            Pattern.compile("(?i)nikto"),
            Pattern.compile("(?i)nessus"),
            Pattern.compile("(?i)masscan"),
            Pattern.compile("(?i)\\bnmap\\b"),
            Pattern.compile("(?i)burpsuite"),
            Pattern.compile("(?i)acunetix"),
            Pattern.compile("(?i)\\bw3af\\b"),
            Pattern.compile("(?i)havij"),
            Pattern.compile("(?i)pangolin"),
            Pattern.compile("(?i)dirbuster"),
            Pattern.compile("(?i)gobuster"),
            Pattern.compile("(?i)wfuzz"),
            Pattern.compile("(?i)nuclei"),
            Pattern.compile("(?i)zgrab")
    );

    private static final List<PatternGroup> URL_PATTERN_GROUPS = List.of(
            new PatternGroup("sql_injection", "critica", List.of(
                    Pattern.compile("(?i)'\\s{0,20}\\)?\\s{0,20}(or|and)\\s{1,20}\\(?'?\\w{1,64}'?\\s{0,20}(=|like)\\s{0,20}'?\\w"),
                    Pattern.compile("(?i)\\bunion\\s{1,20}(all\\s{1,20})?select\\b"),
                    Pattern.compile("(?i);\\s{0,20}(drop|truncate|alter)\\s{1,20}(table|database|schema)\\b"),
                    Pattern.compile("(?i);\\s{0,20}delete\\s{1,20}from\\b"),
                    Pattern.compile("(?i);\\s{0,20}insert\\s{1,20}into\\b"),
                    Pattern.compile("(?i);\\s{0,20}update\\s{1,20}[\\w.\"]{1,64}\\s{1,20}set\\b"),
                    Pattern.compile("[\\w)]'\\s{0,20}\\)?\\s{0,20}(--|#|/\\*)"),
                    Pattern.compile("(?i)\\b(pg_sleep|sleep|benchmark)\\s{0,20}\\(\\s{0,20}\\d"),
                    Pattern.compile("(?i)\\bwaitfor\\s{1,20}delay\\s{1,20}['\"]"),
                    Pattern.compile("(?i)\\bexec(ute)?\\s{1,20}(master\\.\\.)?(xp|sp)_\\w"),
                    Pattern.compile("(?i)\\b(information_schema|pg_catalog)\\s{0,20}\\.\\s{0,20}\\w")
            )),
            new PatternGroup("command_injection", "critica", List.of(
                    Pattern.compile("(?i)[;&|]\\s{0,20}(rm\\s{1,20}-[rf]{1,2}\\b|wget\\s{1,20}https?:|curl\\s{1,20}(-\\w{1,20}\\s{1,20}){0,8}https?:|nc\\s{1,20}(-\\w{1,20}\\s{1,20}){0,8}-e\\b|bash\\s{1,20}-i\\b|sh\\s{1,20}-c\\b|chmod\\s{1,20}\\+x\\b)"),
                    Pattern.compile("(?i)\\$\\(\\s{0,20}(curl\\s{1,20}(-\\w{1,20}\\s{1,20}){0,8}https?:|wget\\s{1,20}https?:|bash\\s{1,20}-|sh\\s{1,20}-c|nc\\s{1,20}-|whoami|cat\\s{1,20}/etc/)[^)]{0,500}\\)"),
                    Pattern.compile("(?i)`\\s{0,20}(curl\\s{1,20}(-\\w{1,20}\\s{1,20}){0,8}https?:|wget\\s{1,20}https?:|nc\\s{1,20}-e|whoami|cat\\s{1,20}/etc/)[^`]{0,500}`"),
                    Pattern.compile("(?i)\\|\\s{0,20}(ba|z)?sh\\b"),
                    Pattern.compile("(?i)/bin/(ba|z)?sh\\b")
            )),
            new PatternGroup("xss", "alta", List.of(
                    Pattern.compile("(?i)<script\\b"),
                    Pattern.compile("(?i)<[a-z][^<>]{0,500}[\\s/\"'](on[a-z]{3,20})\\s{0,20}="),
                    Pattern.compile("(?i)<iframe\\b"),
                    Pattern.compile("(?i)<(object|embed|applet|base|frameset)\\b[^<>]{0,500}\\s(src|data|href|code)\\s{0,20}="),
                    Pattern.compile("(?i)<meta\\b[^<>]{0,500}http-equiv"),
                    Pattern.compile("(?i)\\b(href|src|action|formaction|xlink:href)\\s{0,20}=\\s{0,20}\\\\?[\"']?\\s{0,20}(javascript|vbscript)\\s{0,20}:"),
                    Pattern.compile("(?i)\\bsrcdoc\\s{0,20}="),
                    Pattern.compile("(?i)data\\s{0,20}:\\s{0,20}text/html")
            )),
            new PatternGroup("path_traversal", "alta", List.of(
                    Pattern.compile("(^|[/\\\\=])\\.\\.([/\\\\]|$)"),
                    Pattern.compile("(?i)(%2e%2e|%252e%252e)"),
                    Pattern.compile("(?i)\\.\\.(%c0%af|%c1%9c)"),
                    Pattern.compile("(?i)/etc/(passwd|shadow)\\b"),
                    Pattern.compile("(?i)/proc/self/")
            )),
            new PatternGroup("header_injection", "alta", List.of(
                    Pattern.compile("(?i)%0d%0a"),
                    Pattern.compile("(?i)%0d(%20|\\+)*%0a")
            ))
    );

    private static final Pattern PERCENT_ASCII = Pattern.compile("(?i)%([0-9a-f]{2})");
    private static final Pattern UNICODE_ESCAPE = Pattern.compile("(?i)\\\\u([0-9a-f]{4})");
    private static final Pattern HEX_ENTITY = Pattern.compile("(?i)&#x([0-9a-f]{1,6});?");
    private static final Pattern DEC_ENTITY = Pattern.compile("&#(\\d{1,7});?");
    private static final Pattern NAMED_ENTITY = Pattern.compile("(?i)&(lt|gt|quot|apos|colon|lpar|rpar|tab|newline);");
    private static final Pattern SQL_COMMENT = Pattern.compile("/\\*[^*]{0,200}\\*/");

    private static final Map<String, String> ENTITIES = Map.of(
            "lt", "<",
            "gt", ">",
            "quot", "\"",
            "apos", "'",
            "colon", ":",
            "lpar", "(",
            "rpar", ")",
            "tab", " ",
            "newline", " "
    );

    public Threat inspectUserAgent(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return null;
        }
        for (Pattern pattern : SCANNER_USER_AGENTS) {
            if (pattern.matcher(userAgent).find()) {
                String sample = userAgent.length() > 50 ? userAgent.substring(0, 50) : userAgent;
                return new Threat("suspicious_user_agent", "alta", "ferramenta de varredura: " + sample);
            }
        }
        return null;
    }

    public Threat inspectUrl(String requestUri, String queryString) {
        String raw = (requestUri == null ? "" : requestUri)
                + (queryString == null || queryString.isEmpty() ? "" : "?" + queryString);
        if (raw.isEmpty()) {
            return null;
        }
        String spaceNormalized = raw.replace('+', ' ');
        for (String form : inspectionForms(spaceNormalized)) {
            for (PatternGroup group : URL_PATTERN_GROUPS) {
                for (Pattern pattern : group.patterns()) {
                    if (pattern.matcher(form).find()) {
                        return new Threat(group.type(), group.severity(), "padrão de " + group.type() + " na URL");
                    }
                }
            }
        }
        try {
            URLDecoder.decode(raw.replace("+", "%2B"), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return new Threat("path_traversal", "alta", "URL com codificação percentual inválida");
        }
        return null;
    }

    public static List<String> inspectionForms(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        String unescapedUnicode = replacePattern(text, UNICODE_ESCAPE, hex ->
                String.valueOf((char) Integer.parseInt(hex, 16)));
        String decodedOnce = decodeAsciiPercent(unescapedUnicode);
        String decodedTwice = decodeAsciiPercent(decodedOnce);
        String withHexEntities = replacePattern(decodedTwice, HEX_ENTITY, hex -> {
            int codePoint = Math.min(Integer.parseInt(hex, 16), 0x10FFFF);
            return new String(Character.toChars(codePoint));
        });
        String withDecEntities = replacePattern(withHexEntities, DEC_ENTITY, dec -> {
            int codePoint = Math.min(Integer.parseInt(dec, 10), 0x10FFFF);
            return new String(Character.toChars(codePoint));
        });
        String normalized = replacePattern(withDecEntities, NAMED_ENTITY, name ->
                ENTITIES.getOrDefault(name.toLowerCase(Locale.ROOT), "&" + name + ";"));

        Set<String> forms = new LinkedHashSet<>();
        forms.add(text);
        forms.add(normalized);
        if (normalized.contains("/*")) {
            forms.add(SQL_COMMENT.matcher(normalized).replaceAll(""));
            forms.add(SQL_COMMENT.matcher(normalized).replaceAll(" ").replaceAll("\\s+", " "));
        } else {
            forms.add(normalized.replaceAll("\\s+", " "));
        }
        return new ArrayList<>(forms);
    }

    private static String decodeAsciiPercent(String input) {
        Matcher matcher = PERCENT_ASCII.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            int code = Integer.parseInt(matcher.group(1), 16);
            if (code < 0x80) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf((char) code)));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private interface GroupReplacer {
        String replace(String groupValue);
    }

    private static String replacePattern(String input, Pattern pattern, GroupReplacer replacer) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String replacement = replacer.replace(matcher.group(1));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
