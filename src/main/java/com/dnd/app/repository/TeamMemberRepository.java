package com.dnd.app.repository;

import com.dnd.app.domain.TeamMember;
import com.dnd.app.domain.TeamMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberId> {

    boolean existsByIdTeamIdAndIdPlayerId(UUID teamId, UUID playerId);
}
