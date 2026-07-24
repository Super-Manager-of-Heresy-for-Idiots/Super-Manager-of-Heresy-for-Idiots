package com.dnd.app.config;

import com.dnd.app.exception.ServerBusyException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Класс BoundedExecutorService описывает admission control поверх исполнителя задач контроллеров (N9).
 *
 * <p>Проблема: архитектура сознательно безлимитная (задача = дешёвая виртуальная нить, backpressure —
 * на пуле Hikari). Под флудом ломается не CPU, а память и латентность: неограниченное число запросов
 * «в полёте» держит heap и висит на пуле, деградируют все эндпоинты. Эта обёртка ставит верхний предел
 * числа одновременно принятых задач через {@link Semaphore}:
 * <ul>
 *   <li>{@code tryAcquire(timeout)} выполняется в вызывающем (Tomcat-vthread) потоке — парковка дешёвая,
 *       ожидание до таймаута сглаживает микропики без единого отказа;</li>
 *   <li>не дождались ⇒ {@link ServerBusyException} (синхронно в методе контроллера ⇒ 503 + Retry-After);</li>
 *   <li>получили permit ⇒ задача уходит в делегат, а permit освобождается в {@code finally} после её
 *       выполнения (в т.ч. при исключении — иначе permit «утёк» бы навсегда).</li>
 * </ul>
 *
 * <p>Виртуальные потоки и роль Hikari как точки backpressure сохраняются: обёртка лишь ограничивает
 * число задач в полёте, не вводит фиксированный пул платформенных потоков. Энфорсмент — только в
 * {@link #execute(Runnable)}: контроллеры уходят в асинхрон через {@code CompletableFuture.supplyAsync(..,
 * executor)}, который вызывает именно {@code execute}. Остальные методы {@link ExecutorService}
 * делегируются как есть (в проекте не используются на этом исполнителе).
 */
@Slf4j
public class BoundedExecutorService implements ExecutorService {

    private static final long WARN_THROTTLE_NANOS = TimeUnit.MINUTES.toNanos(1);

    private final ExecutorService delegate;
    private final Semaphore semaphore;
    private final int maxInFlight;
    private final long acquireTimeoutMs;
    private final Counter rejectedCounter;
    private final AtomicLong lastWarnNanos = new AtomicLong(Long.MIN_VALUE);

    /**
     * Создаёт ограниченный исполнитель.
     * @param delegate реальный исполнитель (виртуальные нити), которому передаются принятые задачи
     * @param maxInFlight максимальное число задач «в полёте» (permits семафора)
     * @param acquireTimeoutMs сколько ждать свободный permit перед отказом 503, миллисекунды
     * @param meterRegistry реестр метрик Micrometer для gauge in-flight и counter rejected
     */
    public BoundedExecutorService(ExecutorService delegate, int maxInFlight, long acquireTimeoutMs,
                                  MeterRegistry meterRegistry) {
        this.delegate = delegate;
        this.maxInFlight = maxInFlight;
        this.semaphore = new Semaphore(maxInFlight);
        this.acquireTimeoutMs = acquireTimeoutMs;
        this.rejectedCounter = Counter.builder("app.async.rejected")
                .description("Requests shed by admission control (503 SERVER_BUSY)")
                .register(meterRegistry);
        meterRegistry.gauge("app.async.in.flight", this,
                self -> self.maxInFlight - self.semaphore.availablePermits());
    }

    @Override
    public void execute(Runnable command) {
        boolean acquired;
        try {
            acquired = semaphore.tryAcquire(acquireTimeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServerBusyException("Сервер перегружен, повторите запрос");
        }
        if (!acquired) {
            rejectedCounter.increment();
            warnThrottled();
            throw new ServerBusyException("Сервер перегружен, повторите запрос");
        }
        try {
            delegate.execute(() -> {
                try {
                    command.run();
                } finally {
                    semaphore.release();
                }
            });
        } catch (RuntimeException | Error e) {
            // Делегат отклонил задачу уже после захвата permit — вернём его, чтобы не «утёк».
            semaphore.release();
            throw e;
        }
    }

    /** WARN только на первый shed в минутном окне — защита логов от флуда. */
    private void warnThrottled() {
        long now = System.nanoTime();
        long last = lastWarnNanos.get();
        if (now - last >= WARN_THROTTLE_NANOS && lastWarnNanos.compareAndSet(last, now)) {
            log.warn("Admission control shedding requests: max-in-flight={} exhausted for {} ms",
                    maxInFlight, acquireTimeoutMs);
        }
    }

    // ── Делегирование жизненного цикла и остальных методов ExecutorService ──────────────────────

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return delegate.submit(task);
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return delegate.submit(task, result);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return delegate.submit(task);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return delegate.invokeAll(tasks);
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException {
        return delegate.invokeAll(tasks, timeout, unit);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return delegate.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException, TimeoutException {
        return delegate.invokeAny(tasks, timeout, unit);
    }
}
