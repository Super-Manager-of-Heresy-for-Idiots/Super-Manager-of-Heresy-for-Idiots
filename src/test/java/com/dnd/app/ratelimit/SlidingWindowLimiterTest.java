package com.dnd.app.ratelimit;

import com.github.benmanes.caffeine.cache.Ticker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SlidingWindowLimiter: скользящее окно, TTL-инвариант, независимость ключей")
class SlidingWindowLimiterTest {

    /** Управляемый источник времени: тесты двигают его руками, чтобы не зависеть от реальных часов. */
    private final AtomicLong nanos = new AtomicLong(0);
    private final Ticker ticker = nanos::get;

    private void advance(Duration d) {
        nanos.addAndGet(d.toNanos());
    }

    @Test
    @DisplayName("В пределах окна лимит соблюдается, за окном счётчик сбрасывается")
    void allowsUpToLimitThenResetsAfterWindow() {
        SlidingWindowLimiter limiter = new SlidingWindowLimiter(
                2, Duration.ofMinutes(1), 1000, Duration.ofHours(2), ticker);

        assertThat(limiter.tryAcquire("ip")).isTrue();
        assertThat(limiter.tryAcquire("ip")).isTrue();
        assertThat(limiter.tryAcquire("ip")).isFalse(); // третий в окне — отклонён

        advance(Duration.ofMinutes(1).plusSeconds(1)); // окно проехало
        assertThat(limiter.tryAcquire("ip")).isTrue();
    }

    @Test
    @DisplayName("Разные ключи имеют независимые бакеты")
    void keysAreIndependent() {
        SlidingWindowLimiter limiter = new SlidingWindowLimiter(
                1, Duration.ofMinutes(1), 1000, Duration.ofHours(2), ticker);

        assertThat(limiter.tryAcquire("a")).isTrue();
        assertThat(limiter.tryAcquire("b")).isTrue(); // другой ключ — свой бакет
        assertThat(limiter.tryAcquire("a")).isFalse();
    }

    @Test
    @DisplayName("TTL меньше окна запрещён (иначе счётчик протух бы раньше окна)")
    void rejectsTtlShorterThanWindow() {
        assertThatThrownBy(() -> new SlidingWindowLimiter(
                5, Duration.ofHours(1), 1000, Duration.ofMinutes(30), ticker))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Лимит 0 отклоняет любое попадание")
    void zeroLimitRejectsEverything() {
        SlidingWindowLimiter limiter = new SlidingWindowLimiter(
                0, Duration.ofMinutes(1), 1000, Duration.ofHours(2), ticker);
        assertThat(limiter.tryAcquire("ip")).isFalse();
    }
}
