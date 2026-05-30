package com.dnd.app.repository;

import com.dnd.app.domain.PlayerCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PlayerCharacterRepository extends JpaRepository<PlayerCharacter, UUID> {

    List<PlayerCharacter> findAllByOwnerId(UUID ownerId);

    List<PlayerCharacter> findAllByOwnerIdAndTeamId(UUID ownerId, UUID teamId);

    List<PlayerCharacter> findAllByTeamId(UUID teamId);

    @Query("SELECT pc FROM PlayerCharacter pc WHERE pc.team.gameMaster.id = :gmId")
    List<PlayerCharacter> findAllByGameMasterId(@Param("gmId") UUID gmId);

    @Query("SELECT pc FROM PlayerCharacter pc WHERE pc.team.gameMaster.id = :gmId AND pc.team.id = :teamId")
    List<PlayerCharacter> findAllByGameMasterIdAndTeamId(@Param("gmId") UUID gmId, @Param("teamId") UUID teamId);

    @Query("SELECT CASE WHEN COUNT(tm) > 0 THEN true ELSE false END FROM TeamMember tm " +
           "WHERE tm.id.playerId = :playerId AND tm.team.gameMaster.id = :gmId")
    boolean isPlayerInGameMasterTeam(@Param("playerId") UUID playerId, @Param("gmId") UUID gmId);
}
