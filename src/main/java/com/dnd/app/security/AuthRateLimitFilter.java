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

    private final int loginPerMinute;
    private final int registerPerHour;

    private final ConcurrentHashMap<String, Deque<Instant>> loginHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Deque<Instant>> registerHits = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(
            @Value("${app.ratelimit.login-per-minute:5}") int loginPerMinute,
            @Value("${app.ratelimit.register-per-hour:3}") int registerPerHour
    ) {
        this.loginPerMinute = loginPerMinute;
        this.registerPerHour = registerPerHour;
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

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }
}
