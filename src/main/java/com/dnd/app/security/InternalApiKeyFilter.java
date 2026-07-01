package com.dnd.app.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * Authenticates service-to-service calls to {@code /api/internal/**} with a shared secret carried
 * in the {@code X-Internal-Api-Key} header (map-service -> core BE). A valid key grants the
 * synthetic {@code ROLE_INTERNAL_SERVICE} authority; SecurityConfig then restricts the internal
 * paths to that role, so a missing or wrong key falls through to an unauthenticated request and is
 * rejected. When no key is configured, internal endpoints stay locked down (never authenticated).
 */
@Slf4j
@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Internal-Api-Key";
    public static final String ROLE = "ROLE_INTERNAL_SERVICE";

    private final String configuredKey;

    public InternalApiKeyFilter(@Value("${app.internal.api-key:}") String configuredKey) {
        this.configuredKey = configuredKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path == null || !path.startsWith("/api/internal/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String presented = request.getHeader(HEADER);
        if (StringUtils.hasText(configuredKey) && StringUtils.hasText(presented)
                && constantTimeEquals(configuredKey, presented)) {
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "internal-service", null, List.of(new SimpleGrantedAuthority(ROLE)));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } else {
            log.warn("Rejected internal call without a valid {} header: path={}, remote={}",
                    HEADER, path, request.getRemoteAddr());
        }

        filterChain.doFilter(request, response);
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
