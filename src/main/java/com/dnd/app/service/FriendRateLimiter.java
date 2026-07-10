package com.dnd.app.service;

import com.dnd.app.exception.TooManyRequestsException;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Класс FriendRateLimiter описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Component
public class FriendRateLimiter {

    private final int requestsPerDay;
    private final int searchesPerMinute;
    private final ConcurrentHashMap<UUID, Deque<Instant>> requestHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Deque<Instant>> searchHits = new ConcurrentHashMap<>();

    /**
     * Создает экземпляр компонента домена и получает зависимости, необходимые для выполнения бизнес-логики.
     * @param user входящее значение user, используемое бизнес-сценарием
     * @param requestsPerDay входящие данные запроса для выполнения бизнес-сценария
     * @param searchesPerMinute входящее значение searches per minute, используемое бизнес-сценарием
     */
    public FriendRateLimiter(
    /**
     * Выполняет операции "check friend request" в рамках бизнес-логики домена.
     * @param userId идентификатор user, используемый для выбора нужного бизнес-объекта
     */
            @Value("${app.ratelimit.friend-requests-per-day:30}") int requestsPerDay,
            @Value("${app.ratelimit.user-search-per-minute:30}") int searchesPerMinute) {
        this.requestsPerDay = requestsPerDay;
        this.searchesPerMinute = searchesPerMinute;
    }

    /**
     * Выполняет операции "check friend request" в рамках бизнес-логики домена.
     * @param userId идентификатор user, используемый для выбора нужного бизнес-объекта
     */
    public void checkFriendRequest(UUID userId) {
        if (exceeds(requestHits, userId, requestsPerDay, Duration.ofDays(1))) {
            throw new TooManyRequestsException("Too many friend requests today; try again later.");
        }
    }

    /**
     * Выполняет операции "check user search" в рамках бизнес-логики домена.
     * @param userId идентификатор user, используемый для выбора нужного бизнес-объекта
     */
    public void checkUserSearch(UUID userId) {
        if (exceeds(searchHits, userId, searchesPerMinute, Duration.ofMinutes(1))) {
            throw new TooManyRequestsException("Too many searches; slow down.");
        }
    }

    private boolean exceeds(ConcurrentHashMap<UUID, Deque<Instant>> hits, UUID key, int limit, Duration window) {
        Instant now = Instant.now();
        Instant cutoff = now.minus(window);
        Deque<Instant> deque = hits.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        synchronized (deque) {
            Iterator<Instant> it = deque.iterator();
            while (it.hasNext()) {
                if (it.next().isBefore(cutoff)) {
                    it.remove();
                } else {
                    break;
                }
            }
            if (deque.size() >= limit) {
                log.warn("Friends rate limit exceeded: user={}, count={}, limit={}", key, deque.size(), limit);
                return true;
            }
            deque.addLast(now);
            return false;
        }
    }
}
