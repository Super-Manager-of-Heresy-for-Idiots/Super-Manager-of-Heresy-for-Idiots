package com.dnd.app.repository;

import com.dnd.app.domain.CampaignMember;
import com.dnd.app.domain.PlayerCharacter;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Контракт PlayerCharacterRepository описывает репозиторий, который предоставляет доступ к данным доменной модели.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public interface PlayerCharacterRepository extends JpaRepository<PlayerCharacter, UUID> {

    List<PlayerCharacter> findAllByOwnerId(UUID ownerId);

    @Query("SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END " +
           "FROM PlayerCharacter pc JOIN CampaignMember cm ON cm.campaign = pc.campaign " +
           "WHERE pc.owner.id = :playerId AND cm.user.id = :gmId " +
           "AND cm.roleInCampaign = com.dnd.app.domain.enums.CampaignRole.GM AND cm.kicked = false")
    boolean isPlayerInGameMasterCampaign(@Param("playerId") UUID playerId, @Param("gmId") UUID gmId);

    @EntityGraph(attributePaths = {"owner", "race", "classLevels"})
    List<PlayerCharacter> findByCampaignId(UUID campaignId);

    List<PlayerCharacter> findByCampaignIdAndOwnerId(UUID campaignId, UUID ownerId);

    long countByRaceId(UUID raceId);

    List<PlayerCharacter> findByOwnerIdAndCampaignIsNull(UUID ownerId);

    List<PlayerCharacter> findByBlueprintId(UUID blueprintId);

    List<PlayerCharacter> findByBlueprintIdAndOwnerId(UUID blueprintId, UUID ownerId);

    /**
     * Pessimistic write lock on a character row. Use within @Transactional for
     * mutations that must serialize (XP, level-up, HP changes) to prevent
     * concurrent double-application of irreversible effects.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pc from PlayerCharacter pc where pc.id = :id")
    Optional<PlayerCharacter> findByIdForUpdate(@Param("id") UUID id);
}
