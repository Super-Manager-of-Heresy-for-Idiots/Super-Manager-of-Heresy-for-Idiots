package com.dnd.app.repository;

import com.dnd.app.domain.featurerule.CharacterFeatureChoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт CharacterFeatureChoiceRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CharacterFeatureChoiceRepository extends JpaRepository<CharacterFeatureChoice, UUID> {
    List<CharacterFeatureChoice> findByCharacterId(UUID characterId);
    List<CharacterFeatureChoice> findByCharacterIdAndChoiceGroupId(UUID characterId, UUID choiceGroupId);
    long countByCharacterIdAndChoiceGroupId(UUID characterId, UUID choiceGroupId);
}
