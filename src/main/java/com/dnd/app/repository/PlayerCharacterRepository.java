package com.dnd.app.repository;

import com.dnd.app.domain.CampaignMember;
import com.dnd.app.domain.PlayerCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PlayerCharacterRepository extends JpaRepository<PlayerCharacter, UUID> {

    List<PlayerCharacter> findAllByOwnerId(UUID ownerId);

    @Query("SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END " +
           "FROM PlayerCharacter pc JOIN CampaignMember cm ON cm.campaign = pc.campaign " +
           "WHERE pc.owner.id = :playerId AND cm.user.id = :gmId " +
           "AND cm.roleInCampaign = com.dnd.app.domain.enums.CampaignRole.GM AND cm.kicked = false")
    boolean isPlayerInGameMasterCampaign(@Param("playerId") UUID playerId, @Param("gmId") UUID gmId);

    List<PlayerCharacter> findByCampaignId(UUID campaignId);

    List<PlayerCharacter> findByCampaignIdAndOwnerId(UUID campaignId, UUID ownerId);

    long countByRaceId(UUID raceId);
}
