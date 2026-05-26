package com.dnd.app.repository;

import com.dnd.app.domain.PlayerCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PlayerCharacterRepository extends JpaRepository<PlayerCharacter, UUID> {

    List<PlayerCharacter> findAllByOwnerId(UUID ownerId);

    @Query("SELECT pc FROM PlayerCharacter pc WHERE pc.owner.id IN " +
           "(SELECT tm.id.playerId FROM TeamMember tm WHERE tm.team.gameMaster.id = :gmId)")
    List<PlayerCharacter> findAllByGameMasterId(@Param("gmId") UUID gmId);

    @Query("SELECT CASE WHEN COUNT(tm) > 0 THEN true ELSE false END FROM TeamMember tm " +
           "WHERE tm.id.playerId = :playerId AND tm.team.gameMaster.id = :gmId")
    boolean isPlayerInGameMasterTeam(@Param("playerId") UUID playerId, @Param("gmId") UUID gmId);
}
