package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.CharacterTransformation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Контракт CharacterTransformationRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CharacterTransformationRepository extends JpaRepository<CharacterTransformation, UUID> {
    List<CharacterTransformation> findByCharacterIdAndStatus(UUID characterId, String status);
    List<CharacterTransformation> findByCharacterId(UUID characterId);
    List<CharacterTransformation> findByStatusAndExpiresAtIsNotNullAndExpiresAtBefore(String status, Instant cutoff);
}
