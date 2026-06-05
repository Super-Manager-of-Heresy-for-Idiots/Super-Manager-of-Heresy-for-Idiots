package com.dnd.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_ATTRIBUTE = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
        String path = buildPath(request);
        String routeType = request.getRequestURI().startsWith("/api/") ? "api" : "non-api";
        String remoteAddress = forwardedFor(request);
        long startedAt = System.nanoTime();

        log.info("HTTP request started: id={}, method={}, path={}, routeType={}, remote={}, userAgent='{}'",
                requestId, request.getMethod(), path, routeType, remoteAddress, request.getHeader("User-Agent"));

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            int status = response.getStatus();
            String username = currentUsername(request);

            if (status >= 400) {
                log.warn("HTTP request completed: id={}, method={}, path={}, routeType={}, status={}, durationMs={}, user={}",
                        requestId, request.getMethod(), path, routeType, status, durationMs, username);
            } else {
                log.info("HTTP request completed: id={}, method={}, path={}, routeType={}, status={}, durationMs={}, user={}",
                        requestId, request.getMethod(), path, routeType, status, durationMs, username);
            }
        }
    }

    private String buildPath(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }

    private String forwardedFor(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String currentUsername(HttpServletRequest request) {
        Object authenticatedUsername = request.getAttribute("authenticatedUsername");
        if (authenticatedUsername instanceof String username && !username.isBlank()) {
            return username;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return authentication.getName();
        }
        return request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "anonymous";
    }
}
