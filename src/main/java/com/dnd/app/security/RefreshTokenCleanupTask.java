package com.dnd.app.security;

import com.dnd.app.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Класс RefreshTokenCleanupTask описывает компонент безопасности, который защищает бизнес-сценарии и проверяет доступ пользователя.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupTask {

    private final RefreshTokenRepository refreshTokenRepository;

    /**
     * Выполняет операции "purge expired" в рамках бизнес-логики безопасности.
     */
    @Scheduled(cron = "${app.security.refresh-cleanup-cron:0 30 3 * * *}")
    @Transactional
    public void purgeExpired() {
        int removed = refreshTokenRepository.deleteExpired(Instant.now());
        if (removed > 0) {
            log.info("Purged {} expired refresh tokens", removed);
        }
    }
}
