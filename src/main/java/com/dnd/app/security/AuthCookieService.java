package com.dnd.app.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * Класс AuthCookieService описывает компонент безопасности, который защищает бизнес-сценарии и проверяет доступ пользователя.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
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

    /**
     * Создает экземпляр компонента безопасности и получает зависимости, необходимые для выполнения бизнес-логики.
     * @param accessCookieName входящее значение access cookie name, используемое бизнес-сценарием
     * @param refreshCookieName входящее значение refresh cookie name, используемое бизнес-сценарием
     * @param secure входящее значение secure, используемое бизнес-сценарием
     * @param sameSite входящее значение same site, используемое бизнес-сценарием
     * @param accessPath входящее значение access path, используемое бизнес-сценарием
     * @param refreshPath входящее значение refresh path, используемое бизнес-сценарием
     * @param domain входящее значение domain, используемое бизнес-сценарием
     */
    public AuthCookieService(
    /**
     * Возвращает результат операции "get access cookie name" в рамках бизнес-логики безопасности.
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Возвращает результат операции "get access cookie name" в рамках бизнес-логики безопасности.
     * @return результат выполнения бизнес-операции
     */
    public String getAccessCookieName() {
        return accessCookieName;
    }

    /**
     * Возвращает результат операции "get refresh cookie name" в рамках бизнес-логики безопасности.
     * @return результат выполнения бизнес-операции
     */
    public String getRefreshCookieName() {
        return refreshCookieName;
    }

    /**
     * Выполняет операции "access cookie" в рамках бизнес-логики безопасности.
     * @param token входящее значение token, используемое бизнес-сценарием
     * @param ttlMs входящее значение ttl ms, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public ResponseCookie accessCookie(String token, long ttlMs) {
        return build(accessCookieName, token, accessPath, Duration.ofMillis(ttlMs));
    }

    /**
     * Выполняет операции "refresh cookie" в рамках бизнес-логики безопасности.
     * @param token входящее значение token, используемое бизнес-сценарием
     * @param ttlMs входящее значение ttl ms, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public ResponseCookie refreshCookie(String token, long ttlMs) {
        return build(refreshCookieName, token, refreshPath, Duration.ofMillis(ttlMs));
    }

    /**
     * Выполняет операции "clear access cookie" в рамках бизнес-логики безопасности.
     * @return результат выполнения бизнес-операции
     */
    public ResponseCookie clearAccessCookie() {
        return build(accessCookieName, "", accessPath, Duration.ZERO);
    }

    /**
     * Выполняет операции "clear refresh cookie" в рамках бизнес-логики безопасности.
     * @return результат выполнения бизнес-операции
     */
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
