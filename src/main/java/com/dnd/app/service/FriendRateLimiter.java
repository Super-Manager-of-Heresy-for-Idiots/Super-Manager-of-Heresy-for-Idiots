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
 * Per-user sliding-window rate limiter for the friends endpoints, mirroring the in-memory approach of
 * {@code AuthRateLimitFilter} (per-instance; sufficient for the current single-replica core). See TZ 3.6.
 */
@Slf4j
@Component
public class FriendRateLimiter {

    private final int requestsPerDay;
    private final int searchesPerMinute;
    private final ConcurrentHashMap<UUID, Deque<Instant>> requestHits = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Deque<Instant>> searchHits = new ConcurrentHashMap<>();

    public FriendRateLimiter(
            @Value("${app.ratelimit.friend-requests-per-day:30}") int requestsPerDay,
            @Value("${app.ratelimit.user-search-per-minute:30}") int searchesPerMinute) {
        this.requestsPerDay = requestsPerDay;
        this.searchesPerMinute = searchesPerMinute;
    }

    public void checkFriendRequest(UUID userId) {
        if (exceeds(requestHits, userId, requestsPerDay, Duration.ofDays(1))) {
            throw new TooManyRequestsException("Too many friend requests today; try again later.");
        }
    }

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
