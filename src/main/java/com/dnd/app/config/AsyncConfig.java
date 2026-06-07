package com.dnd.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.concurrent.Executor;

/**
 * Executor used by the REST controllers to run request handling asynchronously
 * (controllers return {@link java.util.concurrent.CompletableFuture}).
 *
 * <p>The {@link TaskDecorator} copies the current {@link SecurityContext} from the
 * request thread onto the worker thread so that anything relying on
 * {@link SecurityContextHolder} keeps working off the request thread.
 */
@Configuration
public class AsyncConfig {

    @Bean
    public Executor controllerTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(32);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("ctrl-async-");
        executor.setTaskDecorator(new SecurityContextTaskDecorator());
        executor.initialize();
        return executor;
    }

    /**
     * Propagates the SecurityContext captured at submission time (the request
     * thread) to the worker thread, restoring the previous context afterwards.
     */
    static final class SecurityContextTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            SecurityContext context = SecurityContextHolder.getContext();
            return () -> {
                SecurityContext previous = SecurityContextHolder.getContext();
                SecurityContextHolder.setContext(context);
                try {
                    runnable.run();
                } finally {
                    SecurityContextHolder.setContext(previous);
                }
            };
        }
    }
}
