package com.dnd.app.repository;

import com.dnd.app.domain.content.CharacterRewardSkillSelection;
import com.dnd.app.domain.content.CharacterRewardSkillSelectionId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Контракт CharacterRewardSkillSelectionRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface CharacterRewardSkillSelectionRepository
        extends JpaRepository<CharacterRewardSkillSelection, CharacterRewardSkillSelectionId> {

    List<CharacterRewardSkillSelection> findAllBySelectionId(UUID selectionId);
}
