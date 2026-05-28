package com.dnd.app.repository;

import com.dnd.app.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    List<Team> findAllByGameMasterId(UUID gameMasterId);

    @Query("SELECT t FROM Team t JOIN t.members m WHERE m.player.id = :playerId")
    List<Team> findAllByPlayerId(@Param("playerId") UUID playerId);

    Optional<Team> findByInviteCode(String inviteCode);
}
