package com.dnd.app.mapper;

import com.dnd.app.domain.Team;
import com.dnd.app.domain.TeamMember;
import com.dnd.app.dto.response.TeamMemberResponse;
import com.dnd.app.dto.response.TeamResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TeamMapper {

    @Mapping(target = "gameMasterId", source = "gameMaster.id")
    @Mapping(target = "gameMasterUsername", source = "gameMaster.username")
    TeamResponse toResponse(Team team);

    @Mapping(target = "playerId", source = "player.id")
    @Mapping(target = "playerUsername", source = "player.username")
    TeamMemberResponse toMemberResponse(TeamMember member);

    List<TeamMemberResponse> toMemberResponseList(List<TeamMember> members);
}
