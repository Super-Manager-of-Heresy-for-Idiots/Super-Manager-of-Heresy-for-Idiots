package com.dnd.app.repository;

import com.dnd.app.domain.content.CharacterRewardSelection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт CharacterRewardSelectionRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CharacterRewardSelectionRepository extends JpaRepository<CharacterRewardSelection, UUID> {

    Optional<CharacterRewardSelection> findByCharacterIdAndRewardGroupId(UUID characterId, UUID groupId);

    Optional<CharacterRewardSelection> findByCharacterIdAndRewardOptionId(UUID characterId, UUID optionId);

    List<CharacterRewardSelection> findAllByCharacterId(UUID characterId);
}
