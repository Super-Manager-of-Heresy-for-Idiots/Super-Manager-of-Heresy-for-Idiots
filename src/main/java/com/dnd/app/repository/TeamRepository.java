package com.dnd.app.repository;

import com.dnd.app.domain.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    List<Team> findAllByGameMasterId(UUID gameMasterId);

    Optional<Team> findByInviteCode(String inviteCode);
}
