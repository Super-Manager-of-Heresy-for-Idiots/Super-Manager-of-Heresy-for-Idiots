package com.dnd.app.security;

import com.dnd.app.ratelimit.ClientIps;
import com.dnd.app.ratelimit.SlidingWindowLimiter;
import com.github.benmanes.caffeine.cache.Ticker;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * Класс AuthRateLimitFilter описывает компонент безопасности, который защищает бизнес-сценарии и проверяет доступ пользователя.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 *
 * <p>N13: пороги, окна, ключи и тексты ошибок не изменились — сменилось только хранилище счётчиков.
 * Четыре {@code ConcurrentHashMap}, растущие без предела, заменены на общий реестр лимитеров
 * {@link SlidingWindowLimiter} поверх Caffeine (ограниченная память, вытеснение неактивных ключей).
 */
@Slf4j
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/auth/login";
    private static final String REGISTER_PATH = "/api/auth/register";
    private static final String REFRESH_PATH = "/api/auth/refresh";
    private static final String SWITCH_PATH = "/api/auth/switch";

    /** TTL неактивного ключа для минутных/часовых окон: 2 часа (>= самого длинного окна — часа). */
    private static final Duration KEY_TTL = Duration.ofHours(2);

    private final int trustedProxyCount;
    private final SlidingWindowLimiter loginLimiter;
    private final SlidingWindowLimiter registerLimiter;
    private final SlidingWindowLimiter refreshLimiter;
    private final SlidingWindowLimiter switchLimiter;

    /**
     * Создаёт компонент безопасности с общим реестром лимитеров (основной конструктор для Spring).
     * @param loginPerMinute порог логинов в минуту на IP
     * @param registerPerHour порог регистраций в час на IP
     * @param refreshPerMinute порог обновлений токена в минуту на IP
     * @param switchPerMinute порог переключений аккаунта в минуту на IP
     * @param trustedProxyCount число доверенных обратных прокси (для разбора X-Forwarded-For)
     * @param authMaxKeys верхняя граница числа IP-ключей в кэше каждого лимитера
     */
    @Autowired
    public AuthRateLimitFilter(
            @Value("${app.ratelimit.login-per-minute:5}") int loginPerMinute,
            @Value("${app.ratelimit.register-per-hour:3}") int registerPerHour,
            @Value("${app.ratelimit.refresh-per-minute:20}") int refreshPerMinute,
            @Value("${app.ratelimit.switch-per-minute:20}") int switchPerMinute,
            @Value("${app.security.trusted-proxy-count:1}") int trustedProxyCount,
            @Value("${app.ratelimit.cache.auth-max-keys:100000}") long authMaxKeys
    ) {
        this.trustedProxyCount = trustedProxyCount;
        this.loginLimiter = new SlidingWindowLimiter(loginPerMinute, Duration.ofMinutes(1), authMaxKeys, KEY_TTL, Ticker.systemTicker());
        this.registerLimiter = new SlidingWindowLimiter(registerPerHour, Duration.ofHours(1), authMaxKeys, KEY_TTL, Ticker.systemTicker());
        this.refreshLimiter = new SlidingWindowLimiter(refreshPerMinute, Duration.ofMinutes(1), authMaxKeys, KEY_TTL, Ticker.systemTicker());
        this.switchLimiter = new SlidingWindowLimiter(switchPerMinute, Duration.ofMinutes(1), authMaxKeys, KEY_TTL, Ticker.systemTicker());
    }

    /**
     * Конструктор-удобство для тестов: те же пороги с дефолтным размером кэша (100 000 ключей).
     * @param loginPerMinute порог логинов в минуту на IP
     * @param registerPerHour порог регистраций в час на IP
     * @param refreshPerMinute порог обновлений токена в минуту на IP
     * @param switchPerMinute порог переключений аккаунта в минуту на IP
     * @param trustedProxyCount число доверенных обратных прокси
     */
    public AuthRateLimitFilter(int loginPerMinute, int registerPerHour, int refreshPerMinute,
                               int switchPerMinute, int trustedProxyCount) {
        this(loginPerMinute, registerPerHour, refreshPerMinute, switchPerMinute, trustedProxyCount, 100_000L);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String ip = ClientIps.resolve(request, trustedProxyCount);

        if (LOGIN_PATH.equals(path) && !loginLimiter.tryAcquire(ip)) {
            reject(response, ip, "login", "Too many login attempts. Try again later.");
            return;
        }
        if (REGISTER_PATH.equals(path) && !registerLimiter.tryAcquire(ip)) {
            reject(response, ip, "register", "Too many registration attempts. Try again later.");
            return;
        }
        if (REFRESH_PATH.equals(path) && !refreshLimiter.tryAcquire(ip)) {
            reject(response, ip, "refresh", "Too many refresh attempts. Try again later.");
            return;
        }
        if (SWITCH_PATH.equals(path) && !switchLimiter.tryAcquire(ip)) {
            reject(response, ip, "switch", "Too many account switch attempts. Try again later.");
            return;
        }

        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, String ip, String endpoint, String message) throws IOException {
        log.warn("Rate limit exceeded for ip={} key=auth endpoint={}", ip, endpoint);
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"message\":\"" + message + "\"}");
    }
}
