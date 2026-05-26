package com.dnd.app.repository;

import com.dnd.app.domain.TeamMember;
import com.dnd.app.domain.TeamMemberId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamMemberRepository extends JpaRepository<TeamMember, TeamMemberId> {
}
