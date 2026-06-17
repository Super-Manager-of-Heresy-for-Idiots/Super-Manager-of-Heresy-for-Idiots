package com.dnd.app.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Phase 11: temporary observability to catch accidental use of the legacy non-bestiary
 * content routes after the new content-model endpoints became the default. Logs a WARN
 * whenever a legacy route is hit so leftover callers are easy to spot before the routes
 * are removed in Phase 12. Read-only: it never blocks or alters requests.
 *
 * <p>Patterns use single-segment {@code *}, so the new {@code .../content/...} routes
 * (which carry an extra path segment) are NOT matched.</p>
 */
@Slf4j
@Configuration
public class LegacyContentRouteLoggingConfig implements WebMvcConfigurer {

    /** Legacy non-bestiary content routes superseded by the new content-model endpoints. */
    private static final List<String> LEGACY_PATTERNS = List.of(
            "/api/reference/classes",
            "/api/campaigns/*/reference/classes",
            "/api/characters/*/level-up-options",
            "/api/characters/*/level-up",
            "/api/characters/full",
            "/api/campaigns/*/characters/full",
            "/api/homebrew/my/*/content/classes/rich",
            "/api/homebrew/my/*/content/classes/import-json",
            "/api/homebrew/my/*/content/classes/*/rich");

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                log.warn("LEGACY content route hit (superseded by new content model): {} {}",
                        request.getMethod(), request.getRequestURI());
                return true;
            }
        }).addPathPatterns(LEGACY_PATTERNS);
    }
}
