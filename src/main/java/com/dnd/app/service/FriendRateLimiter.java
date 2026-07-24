package com.dnd.app.service;

import com.dnd.app.exception.TooManyRequestsException;
import com.dnd.app.ratelimit.SlidingWindowLimiter;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Duration;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Класс FriendRateLimiter описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 *
 * <p>N13/N14: пороги, ключи и тексты ошибок не изменились — две неограниченно растущие
 * {@code ConcurrentHashMap} заменены на общий реестр лимитеров {@link SlidingWindowLimiter} поверх
 * Caffeine (ограниченная память). Точность per-pod остаётся прежней; replica-безопасный потолок на
 * заявки в друзья добирается дешёвым DB-backstop'ом в {@code FriendService.sendFriendRequest}.
 */
@Slf4j
@Component
public class FriendRateLimiter {

    /** TTL ключа заявок должен покрывать суточное окно: 25 часов (> суток). */
    private static final Duration REQUEST_TTL = Duration.ofHours(25);
    /** TTL ключа поиска для минутного окна: 2 часа. */
    private static final Duration SEARCH_TTL = Duration.ofHours(2);

    private final SlidingWindowLimiter requestLimiter;
    private final SlidingWindowLimiter searchLimiter;

    /**
     * Создаёт компонент домена с общим реестром лимитеров.
     * @param requestsPerDay порог заявок в друзья в сутки на пользователя
     * @param searchesPerMinute порог поисков пользователей в минуту на пользователя
     * @param userMaxKeys верхняя граница числа user-ключей в кэше каждого лимитера
     */
    public FriendRateLimiter(
            @Value("${app.ratelimit.friend-requests-per-day:30}") int requestsPerDay,
            @Value("${app.ratelimit.user-search-per-minute:30}") int searchesPerMinute,
            @Value("${app.ratelimit.cache.user-max-keys:50000}") long userMaxKeys) {
        this.requestLimiter = new SlidingWindowLimiter(requestsPerDay, Duration.ofDays(1), userMaxKeys, REQUEST_TTL, Ticker.systemTicker());
        this.searchLimiter = new SlidingWindowLimiter(searchesPerMinute, Duration.ofMinutes(1), userMaxKeys, SEARCH_TTL, Ticker.systemTicker());
    }

    /**
     * Выполняет операции "check friend request" в рамках бизнес-логики домена.
     * @param userId идентификатор user, используемый для выбора нужного бизнес-объекта
     */
    public void checkFriendRequest(UUID userId) {
        if (!requestLimiter.tryAcquire(userId.toString())) {
            log.warn("Friends rate limit exceeded: user={}, key=friend-request", userId);
            throw new TooManyRequestsException("Too many friend requests today; try again later.");
        }
    }

    /**
     * Выполняет операции "check user search" в рамках бизнес-логики домена.
     * @param userId идентификатор user, используемый для выбора нужного бизнес-объекта
     */
    public void checkUserSearch(UUID userId) {
        if (!searchLimiter.tryAcquire(userId.toString())) {
            log.warn("Friends rate limit exceeded: user={}, key=user-search", userId);
            throw new TooManyRequestsException("Too many searches; slow down.");
        }
    }
}
