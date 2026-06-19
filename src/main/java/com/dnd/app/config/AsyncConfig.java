package com.dnd.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Async execution strategy for the application.
 *
 * <p>The application runs on Java 21 virtual threads ({@code spring.threads.virtual.enabled=true}),
 * so a blocking call (JDBC, etc.) parks the virtual thread and frees its carrier platform
 * thread for other work. There is therefore no need — and it is actively harmful — to funnel
 * requests through a fixed-size platform-thread pool: that would cap concurrency at the pool
 * size regardless of how many virtual threads the runtime could otherwise run.
 *
 * <p>{@link #controllerTaskExecutor()} is consequently backed by a
 * {@code newVirtualThreadPerTaskExecutor()}: it is unbounded, spawns one cheap virtual thread
 * per task, and lets concurrency scale with offered load. The real back-pressure ceiling for
 * request handling is the JDBC connection pool (see HikariCP settings in application.yml), which
 * is the correct place to bound work — not the thread layer.
 *
 * <p>Scaling across pods / resources: virtual-thread carriers default to
 * {@code Runtime.availableProcessors()}, which honours container CPU limits, so each pod
 * automatically scales its parallelism to the CPU it was granted. Memory is the practical
 * bound on in-flight virtual threads; size pod memory and the DB pool together.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Backs the {@code CompletableFuture} offloading in the controllers. Wrapped with
     * {@link DelegatingSecurityContextExecutorService} so the Spring Security context of the
     * request thread propagates to the worker thread (the global exception handler and any
     * security-aware code keep working off-request-thread).
     */
    @Bean(destroyMethod = "shutdown")
    public Executor controllerTaskExecutor() {
        return new DelegatingSecurityContextExecutorService(
                Executors.newVirtualThreadPerTaskExecutor());
    }
}
