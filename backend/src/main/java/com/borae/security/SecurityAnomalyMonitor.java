package com.borae.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

/**
 * Monitor de anomalias e sinais de automação, adaptado de {@code src/anomalias.ts} do Neves Shield.
 *
 * <p>Detecta varredura de rotas, insistência após recusas e sinais de clientes automatizados,
 * gerando alertas no canal de segurança sem bloquear IPs compartilhados.</p>
 */
@Component
public class SecurityAnomalyMonitor {

    private static final Pattern SCRIPT_CLIENT_PATTERN = Pattern.compile(
            "(?i)(curl/|wget/|python-requests|aiohttp|httpx|axios/|node-fetch|go-http-client|okhttp|scrapy|playwright|puppeteer|selenium)"
    );
    private static final Pattern HEADLESS_PATTERN = Pattern.compile("(?i)HeadlessChrome");
    private static final Pattern CHROME_PATTERN = Pattern.compile("Chrome/\\d");
    private static final Pattern EDGE_OPERA_PATTERN = Pattern.compile("Edg|OPR");

    private static final int MAX_TRACKED_ROUTES_PER_IP = 200;

    private final long windowMs;
    private final int routeThreshold;
    private final int rejectionThreshold;
    private final int automationThreshold;
    private final int maxTrackedIps;
    private final LongSupplier clock;
    private final Map<String, IpTrace> traces = new LinkedHashMap<>();

    public record Anomaly(String type, String detail) {
    }

    private static final class IpTrace {
        final long windowStartMs;
        final Set<String> routes = new HashSet<>();
        int rejections;
        int automationCount;
        final Set<String> signals = new HashSet<>();
        final Set<String> alertedTypes = new HashSet<>();

        IpTrace(long windowStartMs) {
            this.windowStartMs = windowStartMs;
        }
    }

    public SecurityAnomalyMonitor() {
        this(5 * 60 * 1000L, 30, 20, 40, 5000, System::currentTimeMillis);
    }

    public SecurityAnomalyMonitor(
            long windowMs,
            int routeThreshold,
            int rejectionThreshold,
            int automationThreshold,
            int maxTrackedIps,
            LongSupplier clock
    ) {
        this.windowMs = windowMs;
        this.routeThreshold = routeThreshold;
        this.rejectionThreshold = rejectionThreshold;
        this.automationThreshold = automationThreshold;
        this.maxTrackedIps = maxTrackedIps;
        this.clock = clock;
    }

    public static List<String> detectAutomationSignals(HttpServletRequest request) {
        if (request == null) {
            return List.of();
        }
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            userAgent = "";
        }
        List<String> signals = new ArrayList<>(4);
        if (HEADLESS_PATTERN.matcher(userAgent).find()) {
            signals.add("navegador_headless");
        }
        if (SCRIPT_CLIENT_PATTERN.matcher(userAgent).find()) {
            signals.add("cliente_de_script");
        }
        if (CHROME_PATTERN.matcher(userAgent).find()
                && !EDGE_OPERA_PATTERN.matcher(userAgent).find()
                && request.getHeader("sec-ch-ua") == null) {
            signals.add("chrome_sem_client_hints");
        }
        String method = request.getMethod();
        if (!"GET".equalsIgnoreCase(method)
                && !"HEAD".equalsIgnoreCase(method)
                && userAgent.contains("Mozilla/")
                && request.getHeader("sec-fetch-site") == null) {
            signals.add("escrita_sem_sec_fetch");
        }
        return signals;
    }

    public synchronized Anomaly observe(String ip, String route, boolean rejected, List<String> signals) {
        long now = clock.getAsLong();
        IpTrace trace = getOrCreateTrace(ip, now);

        if (trace.routes.size() < MAX_TRACKED_ROUTES_PER_IP && route != null) {
            trace.routes.add(route);
        }
        if (rejected) {
            trace.rejections++;
        }
        if (signals != null && !signals.isEmpty()) {
            trace.automationCount++;
            trace.signals.addAll(signals);
        }

        if (trace.routes.size() >= routeThreshold && trace.alertedTypes.add("varredura")) {
            return new Anomaly("varredura", "varredura: " + trace.routes.size() + " rotas diferentes na janela");
        }
        if (trace.rejections >= rejectionThreshold && trace.alertedTypes.add("insistencia")) {
            return new Anomaly("insistencia", "insistência: " + trace.rejections + " recusas na janela");
        }
        if (trace.automationCount >= automationThreshold && trace.alertedTypes.add("automacao")) {
            return new Anomaly("automacao", "automação: " + trace.automationCount
                    + " pedidos com sinais (" + String.join(", ", trace.signals) + ")");
        }
        return null;
    }

    public synchronized void reset() {
        traces.clear();
    }

    private IpTrace getOrCreateTrace(String ip, long now) {
        String safeIp = (ip == null || ip.isBlank()) ? ClientIpResolver.UNKNOWN_IP : ip;
        IpTrace existing = traces.get(safeIp);
        if (existing != null && (now - existing.windowStartMs) < windowMs) {
            return existing;
        }
        if (existing != null) {
            traces.remove(safeIp);
        }
        if (traces.size() >= maxTrackedIps) {
            Iterator<String> iterator = traces.keySet().iterator();
            if (iterator.hasNext()) {
                iterator.next();
                iterator.remove();
            }
        }
        IpTrace created = new IpTrace(now);
        traces.put(safeIp, created);
        return created;
    }
}
