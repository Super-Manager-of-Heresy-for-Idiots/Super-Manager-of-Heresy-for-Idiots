package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.CharacterFeatureCompanion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт CharacterFeatureCompanionRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CharacterFeatureCompanionRepository extends JpaRepository<CharacterFeatureCompanion, UUID> {
    List<CharacterFeatureCompanion> findByCharacterId(UUID characterId);
}
