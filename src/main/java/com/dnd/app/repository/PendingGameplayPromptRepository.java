package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.PendingGameplayPrompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Контракт PendingGameplayPromptRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface PendingGameplayPromptRepository extends JpaRepository<PendingGameplayPrompt, UUID> {
    List<PendingGameplayPrompt> findByCharacterIdAndStatus(UUID characterId, String status);
    List<PendingGameplayPrompt> findByStatusAndExpiresAtIsNotNullAndExpiresAtBefore(String status, Instant cutoff);
}
