package com.dnd.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String REFRESH_PATH = "/api/auth/refresh";

    private final int loginPerMinute;
    private final int registerPerHour;
    private final int refreshPerMinute;
    private final int trustedProxyCount;

    private final ConcurrentHashMap<String, Deque<Instant>> loginHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Instant>> registerHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Instant>> refreshHits = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(
            @Value("${app.ratelimit.login-per-minute:5}") int loginPerMinute,
            @Value("${app.ratelimit.register-per-hour:3}") int registerPerHour,
            @Value("${app.ratelimit.refresh-per-minute:20}") int refreshPerMinute,
            @Value("${app.security.trusted-proxy-count:1}") int trustedProxyCount
    ) {
        this.loginPerMinute = loginPerMinute;
        this.registerPerHour = registerPerHour;
        this.refreshPerMinute = refreshPerMinute;
        this.trustedProxyCount = trustedProxyCount;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String ip = clientIp(request);

        if (LOGIN_PATH.equals(path) && exceeds(loginHits, ip, loginPerMinute, Duration.ofMinutes(1))) {
            reject(response, "Too many login attempts. Try again later.");
            return;
        }
        if (REGISTER_PATH.equals(path) && exceeds(registerHits, ip, registerPerHour, Duration.ofHours(1))) {
            reject(response, "Too many registration attempts. Try again later.");
            return;
        }
        if (REFRESH_PATH.equals(path) && exceeds(refreshHits, ip, refreshPerMinute, Duration.ofMinutes(1))) {
            reject(response, "Too many refresh attempts. Try again later.");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean exceeds(ConcurrentHashMap<String, Deque<Instant>> hits, String key, int limit, Duration window) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);
        Deque<Instant> deque = hits.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (deque) {
            Iterator<Instant> it = deque.iterator();
            while (it.hasNext()) {
                if (it.next().isBefore(cutoff)) {
                    it.remove();
                } else {
                    break;
                }
            }
            if (deque.size() >= limit) {
                log.warn("Rate limit exceeded for ip={} key=auth count={} limit={}", key, deque.size(), limit);
                return true;
            }
            deque.addLast(now);
            return false;
        }
    }

    private void reject(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
    }

    /**
     * Resolves the client IP used as the rate-limit key. The left side of {@code X-Forwarded-For}
     * is attacker-controlled (a client may prepend arbitrary hops), so the leftmost value must
     * never be trusted. With {@code trustedProxyCount} reverse proxies in front of the app — each
     * appending the downstream peer's address (e.g. nginx {@code $proxy_add_x_forwarded_for}) — the
     * genuine client IP is the hop our outermost proxy appended, at index
     * {@code length - trustedProxyCount}. A header shorter than expected (forged/stripped, or no
     * proxy in front) falls back to the transport remote address.
     */
    private String clientIp(HttpServletRequest request) {
        if (trustedProxyCount > 0) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isBlank()) {
                String[] hops = forwarded.split(",");
                int idx = hops.length - trustedProxyCount;
                if (idx >= 0 && idx < hops.length) {
                    String candidate = hops[idx].trim();
                    if (!candidate.isEmpty()) {
                        return candidate;
                    }
                }
            }
        }
        return request.getRemoteAddr();
    }
}
