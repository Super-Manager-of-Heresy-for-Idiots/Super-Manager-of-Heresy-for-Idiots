package com.dnd.app.repository;

import com.dnd.app.domain.CharacterQuestObjectiveProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт CharacterQuestObjectiveProgressRepository описывает репозиторий прогресса персонажа
 * по целям квеста (WORLD_PLAN Этап 3).
 */
public interface CharacterQuestObjectiveProgressRepository
        extends JpaRepository<CharacterQuestObjectiveProgress, UUID> {

    List<CharacterQuestObjectiveProgress> findByCharacterQuestId(UUID characterQuestId);

    Optional<CharacterQuestObjectiveProgress> findByCharacterQuestIdAndObjectiveId(UUID characterQuestId, UUID objectiveId);
}
