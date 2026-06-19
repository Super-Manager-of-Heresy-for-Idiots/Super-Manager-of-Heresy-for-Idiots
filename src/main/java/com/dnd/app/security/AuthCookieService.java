package com.dnd.app.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Builds the HttpOnly session cookies that carry the JWT access/refresh tokens.
 *
 * Design notes:
 * - Both cookies are HttpOnly so JS (and any XSS payload) cannot read the tokens.
 * - The refresh cookie is path-scoped to /api/auth so it is only ever sent to the
 *   refresh/logout endpoints, never on ordinary API traffic.
 * - Secure / SameSite are configurable: in local http dev Secure must be false, in
 *   production behind https it should be true (and SameSite=None if FE and API are
 *   on different sites).
 */
@Component
public class AuthCookieService {

    private final String accessCookieName;
    private final String refreshCookieName;
    private final boolean secure;
    private final String sameSite;
    private final String accessPath;
    private final String refreshPath;
    private final String domain;

    public AuthCookieService(
            @Value("${app.jwt.access-cookie-name:access_token}") String accessCookieName,
            @Value("${app.jwt.refresh-cookie-name:refresh_token}") String refreshCookieName,
            @Value("${app.jwt.cookie.secure:false}") boolean secure,
            @Value("${app.jwt.cookie.same-site:Lax}") String sameSite,
            @Value("${app.jwt.cookie.access-path:/}") String accessPath,
            @Value("${app.jwt.cookie.refresh-path:/api/auth}") String refreshPath,
            @Value("${app.jwt.cookie.domain:}") String domain) {
        this.accessCookieName = accessCookieName;
        this.refreshCookieName = refreshCookieName;
        this.secure = secure;
        this.sameSite = sameSite;
        this.accessPath = accessPath;
        this.refreshPath = refreshPath;
        this.domain = domain;
    }

    public String getAccessCookieName() {
        return accessCookieName;
    }

    public String getRefreshCookieName() {
        return refreshCookieName;
    }

    public ResponseCookie accessCookie(String token, long ttlMs) {
        return build(accessCookieName, token, accessPath, Duration.ofMillis(ttlMs));
    }

    public ResponseCookie refreshCookie(String token, long ttlMs) {
        return build(refreshCookieName, token, refreshPath, Duration.ofMillis(ttlMs));
    }

    public ResponseCookie clearAccessCookie() {
        return build(accessCookieName, "", accessPath, Duration.ZERO);
    }

    public ResponseCookie clearRefreshCookie() {
        return build(refreshCookieName, "", refreshPath, Duration.ZERO);
    }

    private ResponseCookie build(String name, String value, String path, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path(path)
                .maxAge(maxAge);
        if (StringUtils.hasText(domain)) {
            builder.domain(domain);
        }
        return builder.build();
    }
}
