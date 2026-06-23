package com.dnd.app.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Forces the deferred CSRF token to materialize on every request so the
 * XSRF-TOKEN cookie is actually written to the response.
 *
 * Spring Security 6 loads the CSRF token lazily — on a plain GET that never reads
 * the token, CookieCsrfTokenRepository would never emit the cookie, leaving the SPA
 * with nothing to echo on its first mutation. Touching getToken() here triggers the
 * repository to set the cookie.
 */
final class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Reading the value causes the deferred token to load and the cookie to be set.
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
