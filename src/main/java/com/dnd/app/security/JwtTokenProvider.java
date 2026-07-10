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
import java.util.UUID;

/**
 * Класс JwtTokenProvider описывает компонент безопасности, который защищает бизнес-сценарии и проверяет доступ пользователя.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Component
public class JwtTokenProvider {

    /** Distinguishes short-lived API tokens from long-lived renewal tokens. */
    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";
    private static final String CLAIM_TYPE = "tt";
    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_JTI = "jti";
    private static final String CLAIM_USER_ID = "user_id";

    private final SecretKey key;
    private final long expirationMs;
    private final long refreshExpirationMs;

    /**
     * Создает экземпляр компонента безопасности и получает зависимости, необходимые для выполнения бизнес-логики.
     * @param secret входящее значение secret, используемое бизнес-сценарием
     * @param expirationMs входящее значение expiration ms, используемое бизнес-сценарием
     * @param refreshExpirationMs входящее значение refresh expiration ms, используемое бизнес-сценарием
     */
    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs,
            @Value("${app.jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        log.info("JwtTokenProvider initialized, accessTTL={}ms refreshTTL={}ms", expirationMs, refreshExpirationMs);
    }

    /**
     * Выполняет операции "generate token" в рамках бизнес-логики безопасности.
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param role входящее значение role, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public String generateToken(String username, String role) {
        return generateToken(username, role, null);
    }

    /**
     * Выполняет операции "generate token" в рамках бизнес-логики безопасности.
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param role входящее значение role, используемое бизнес-сценарием
     * @param userId идентификатор user, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    public String generateToken(String username, String role, UUID userId) {
        return build(username, role, TYPE_ACCESS, expirationMs, userId, null);
    }

    /**
     * Выполняет операции "generate refresh token" в рамках бизнес-логики безопасности.
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param role входящее значение role, используемое бизнес-сценарием
     * @param jti входящее значение jti, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public String generateRefreshToken(String username, String role, String jti) {
        return generateRefreshToken(username, role, null, jti);
    }

    /**
     * Выполняет операции "generate refresh token" в рамках бизнес-логики безопасности.
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param role входящее значение role, используемое бизнес-сценарием
     * @param userId идентификатор user, используемый для выбора нужного бизнес-объекта
     * @param jti входящее значение jti, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public String generateRefreshToken(String username, String role, UUID userId, String jti) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + refreshExpirationMs);
        var builder = Jwts.builder()
                .subject(username)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, TYPE_REFRESH)
                .claim(CLAIM_JTI, jti)
                .issuedAt(now)
                .expiration(expiry);
        if (userId != null) {
            builder.claim(CLAIM_USER_ID, userId.toString());
        }
        return builder.signWith(key, Jwts.SIG.HS256).compact();
    }

    private String build(String username, String role, String type, long ttlMs, UUID userId, String jti) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + ttlMs);
        var builder = Jwts.builder()
                .subject(username)
                .claim(CLAIM_ROLE, role)
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(expiry);
        if (userId != null) {
            builder.claim(CLAIM_USER_ID, userId.toString());
        }
        if (jti != null) {
            builder.claim(CLAIM_JTI, jti);
        }
        return builder.signWith(key, Jwts.SIG.HS256).compact();
    }

    /**
     * Возвращает результат операции "get username from token" в рамках бизнес-логики безопасности.
     * @param token входящее значение token, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Возвращает результат операции "get role from token" в рамках бизнес-логики безопасности.
     * @param token входящее значение token, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public String getRoleFromToken(String token) {
        return parseClaims(token).get(CLAIM_ROLE, String.class);
    }

    /**
     * Возвращает результат операции "get token type" в рамках бизнес-логики безопасности.
     * @param token входящее значение token, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public String getTokenType(String token) {
        return parseClaims(token).get(CLAIM_TYPE, String.class);
    }

    /**
     * Возвращает результат операции "get jti" в рамках бизнес-логики безопасности.
     * @param token входящее значение token, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public String getJti(String token) {
        return parseClaims(token).get(CLAIM_JTI, String.class);
    }

    /**
     * Проверяет корректность операции "validate token" в рамках бизнес-логики безопасности.
     * @param token входящее значение token, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception e) {
            log.warn(
                    "JwtTokenProvider#validateToken failed: operation=jwt-validate, exception={}, message='{}'",
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e);
            return false;
        }
    }

    /**
     * Проверяет условие операции "is access token" в рамках бизнес-логики безопасности.
     * @param token входящее значение token, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public boolean isAccessToken(String token) {
        return isOfType(token, TYPE_ACCESS);
    }

    /**
     * Проверяет условие операции "is refresh token" в рамках бизнес-логики безопасности.
     * @param token входящее значение token, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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
            log.warn(
                    "JwtTokenProvider#isOfType failed: operation=jwt-type-check, expectedType={}, exception={}, message='{}'",
                    expectedType,
                    e.getClass().getSimpleName(),
                    e.getMessage(),
                    e);
            return false;
        }
    }

    /**
     * Возвращает результат операции "get expiration ms" в рамках бизнес-логики безопасности.
     * @return результат выполнения бизнес-операции
     */
    public long getExpirationMs() {
        return expirationMs;
    }

    /**
     * Возвращает результат операции "get refresh expiration ms" в рамках бизнес-логики безопасности.
     * @return результат выполнения бизнес-операции
     */
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
