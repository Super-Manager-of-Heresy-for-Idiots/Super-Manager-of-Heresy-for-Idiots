package com.dnd.app.repository;

import com.dnd.app.domain.CharacterQuest;
import com.dnd.app.domain.enums.CharacterQuestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт CharacterQuestRepository описывает репозиторий журнала квестов персонажа
 * (WORLD_PLAN Этап 2). Используется игровым флоу взятия/сдачи квестов у NPC.
 */
public interface CharacterQuestRepository extends JpaRepository<CharacterQuest, UUID> {

    @EntityGraph(attributePaths = {"quest", "givenByNpc"})
    List<CharacterQuest> findByCharacterId(UUID characterId);

    @EntityGraph(attributePaths = {"character"})
    List<CharacterQuest> findByQuestId(UUID questId);

    Optional<CharacterQuest> findByCharacterIdAndQuestId(UUID characterId, UUID questId);

    List<CharacterQuest> findByQuestIdAndCharacterIdIn(UUID questId, List<UUID> characterIds);

    /** Записи журнала в заданном статусе по кампании квеста — для ГМ-списка ожидающих сдач. */
    @EntityGraph(attributePaths = {"quest", "character", "givenByNpc"})
    List<CharacterQuest> findByStatusAndQuestCampaignId(CharacterQuestStatus status, UUID campaignId);
}
