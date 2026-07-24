package com.dnd.app.config;

import com.dnd.app.exception.ServerBusyException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("BoundedExecutorService: admission control сбрасывает избыток и не теряет permit'ы")
class BoundedExecutorServiceTest {

    @Test
    @DisplayName("Исчерпание слотов даёт ServerBusyException и инкремент counter'а rejected")
    void shedsWhenNoPermitAvailable() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ExecutorService delegate = Executors.newVirtualThreadPerTaskExecutor();
        BoundedExecutorService bounded = new BoundedExecutorService(delegate, 1, 100, registry);

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        bounded.execute(() -> {
            started.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        assertThat(started.await(2, TimeUnit.SECONDS)).isTrue();

        // Единственный permit занят: следующая задача сбрасывается по таймауту.
        assertThatThrownBy(() -> bounded.execute(() -> { }))
                .isInstanceOf(ServerBusyException.class);
        assertThat(registry.get("app.async.rejected").counter().count()).isEqualTo(1.0);

        release.countDown();
        bounded.shutdown();
    }

    @Test
    @DisplayName("Исключение из задачи освобождает permit (нет утечки слота)")
    void releasesPermitWhenTaskThrows() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ExecutorService delegate = Executors.newVirtualThreadPerTaskExecutor();
        BoundedExecutorService bounded = new BoundedExecutorService(delegate, 1, 2000, registry);

        CountDownLatch firstRan = new CountDownLatch(1);
        bounded.execute(() -> {
            firstRan.countDown();
            throw new RuntimeException("boom");
        });
        assertThat(firstRan.await(2, TimeUnit.SECONDS)).isTrue();

        // Если permit «утёк», следующая задача не получит слот; ждём в пределах acquire-timeout.
        CountDownLatch secondRan = new CountDownLatch(1);
        bounded.execute(secondRan::countDown);
        assertThat(secondRan.await(2, TimeUnit.SECONDS)).isTrue();

        bounded.shutdown();
    }
}
