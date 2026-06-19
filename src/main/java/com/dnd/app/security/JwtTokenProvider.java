package com.dnd.app.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    /** Distinguishes short-lived API tokens from long-lived renewal tokens. */
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";
    private static final String CLAIM_TYPE = "tt";
    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final long expirationMs;
    private final long refreshExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        log.info("JwtTokenProvider initialized, accessTTL={}ms refreshTTL={}ms", expirationMs, refreshExpirationMs);
    }

    /** Access token: presented on every API/WS call, short TTL. */
    public String generateToken(String username, String role) {
        return build(username, role, TYPE_ACCESS, expirationMs);
    }

    /** Refresh token: only accepted by /api/auth/refresh, long TTL, never used to authorize requests. */
    public String generateRefreshToken(String username, String role) {
        return build(username, role, TYPE_REFRESH, refreshExpirationMs);
    }

    private String build(String username, String role, String type, long ttlMs) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);
        return Jwts.builder()
                .subject(username)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public String getRoleFromToken(String token) {
        return parseClaims(token).get(CLAIM_ROLE, String.class);
    }

    public String getTokenType(String token) {
        return parseClaims(token).get(CLAIM_TYPE, String.class);
    }

    /** Generic validity (signature + expiry). Does not check token type. */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.warn("JWT validation failed: {} — {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    /** Valid signature, not expired, AND carries the access type. Used by the request filter. */
    public boolean isAccessToken(String token) {
        return isOfType(token, TYPE_ACCESS);
    }

    /** Valid signature, not expired, AND carries the refresh type. Used by /api/auth/refresh. */
    public boolean isRefreshToken(String token) {
        return isOfType(token, TYPE_REFRESH);
    }

    private boolean isOfType(String token, String expectedType) {
        try {
            // Legacy tokens minted before token typing carry no "tt" claim. Treat a missing
            // type as access so already-issued sessions keep working until they expire.
            String type = parseClaims(token).get(CLAIM_TYPE, String.class);
            if (type == null) {
                return TYPE_ACCESS.equals(expectedType);
            }
            return expectedType.equals(type);
        } catch (Exception e) {
            log.warn("JWT type check failed: {} — {}", e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public long getRefreshExpirationMs() {
        return refreshExpirationMs;
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
