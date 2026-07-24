package com.dnd.app.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Класс SlidingWindowLimiter описывает лимитер скользящего окна поверх Caffeine-кэша.
 *
 * <p>Логика: на каждый ключ хранится очередь наносекундных отметок попаданий (Deque под
 * {@code synchronized}), из которой при каждой проверке вычищаются отметки старше окна; если
 * оставшихся отметок меньше лимита — попадание разрешается и записывается, иначе отклоняется.
 * Это тот же алгоритм, что раньше жил в {@code AuthRateLimitFilter}/{@code FriendRateLimiter}, но
 * хранилище очередей — {@link Caffeine} с {@code maximumSize} и {@code expireAfterAccess}, поэтому
 * память ограничена сверху, а неактивные ключи вытесняются (раньше карты росли без предела — N13).
 *
 * <p>Обязательный инвариант: {@code ttl >= window}. Если TTL меньше окна, запись ключа истечёт
 * раньше, чем закроется окно лимита, и счётчик обнулится преждевременно — лимит ослабнет. Конструктор
 * проверяет это условие. Время берётся из {@link Ticker} (и для окна, и для протухания Caffeine),
 * что позволяет детерминированно тестировать лимитер с подставным тикером.
 */
public class SlidingWindowLimiter {

    private final int limit;
    private final long windowNanos;
    private final Ticker ticker;
    private final Cache<String, Deque<Long>> buckets;

    /**
     * Создаёт лимитер скользящего окна.
     * @param limit максимально допустимое число попаданий в пределах окна (при {@code <= 0} лимитер
     *              отклоняет каждое попадание)
     * @param window длительность скользящего окна лимита
     * @param maxKeys верхняя граница числа ключей в кэше (LRU-вытеснение сверх неё)
     * @param ttl время жизни неактивного ключа; ДОЛЖНО быть {@code >= window}
     * @param ticker источник времени для окна и протухания кэша (обычно {@link Ticker#systemTicker()})
     */
    public SlidingWindowLimiter(int limit, Duration window, long maxKeys, Duration ttl, Ticker ticker) {
        if (ttl.compareTo(window) < 0) {
            throw new IllegalArgumentException("ttl (" + ttl + ") must be >= window (" + window + ")");
        }
        this.limit = limit;
        this.windowNanos = window.toNanos();
        this.ticker = ticker;
        this.buckets = Caffeine.newBuilder()
                .maximumSize(maxKeys)
                .expireAfterAccess(ttl)
                .ticker(ticker)
                .build();
    }

    /**
     * Пытается зарегистрировать одно попадание по ключу.
     * @param key ключ лимита (IP или идентификатор пользователя)
     * @return {@code true}, если попадание в пределах лимита (и оно учтено), {@code false}, если лимит
     *         уже исчерпан в текущем окне (попадание не учитывается)
     */
    public boolean tryAcquire(String key) {
        long now = ticker.read();
        long cutoff = now - windowNanos;
        Deque<Long> deque = buckets.get(key, k -> new ArrayDeque<>());
        synchronized (deque) {
            Iterator<Long> it = deque.iterator();
            while (it.hasNext()) {
                if (it.next() < cutoff) {
                    it.remove();
                } else {
                    break;
                }
            }
            if (deque.size() >= limit) {
                return false;
            }
            deque.addLast(now);
            return true;
        }
    }
}
