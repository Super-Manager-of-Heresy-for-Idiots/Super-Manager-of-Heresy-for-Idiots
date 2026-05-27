package com.dnd.app.service;

import com.dnd.app.domain.Team;
import com.dnd.app.domain.TeamMemberId;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.CreateTeamRequest;
import com.dnd.app.dto.request.JoinTeamRequest;
import com.dnd.app.dto.response.TeamResponse;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.TeamMapper;
import com.dnd.app.repository.TeamMemberRepository;
import com.dnd.app.repository.TeamRepository;
import com.dnd.app.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private TeamMapper teamMapper;

    @InjectMocks private TeamService teamService;

    @Test
    void createTeam_success() {
        UUID gmId = UUID.randomUUID();
        User gm = User.builder().id(gmId).username("gm1").role(Role.GAME_MASTER).build();
        when(userRepository.findByUsername("gm1")).thenReturn(Optional.of(gm));

        Team saved = Team.builder().id(UUID.randomUUID()).name("Party A").gameMaster(gm).inviteCode("ABCD1234").build();
        when(teamRepository.save(any(Team.class))).thenReturn(saved);
        TeamResponse expected = TeamResponse.builder().name("Party A").build();
        when(teamMapper.toResponse(saved)).thenReturn(expected);

        CreateTeamRequest req = CreateTeamRequest.builder().name("Party A").build();
        TeamResponse result = teamService.createTeam(req, "gm1");

        assertEquals("Party A", result.getName());
        verify(teamRepository).save(any(Team.class));
    }

    @Test
    void joinTeam_validCode_success() {
        UUID playerId = UUID.randomUUID();
        UUID teamId = UUID.randomUUID();
        User player = User.builder().id(playerId).username("player1").role(Role.PLAYER).build();
        User gm = User.builder().id(UUID.randomUUID()).username("gm1").role(Role.GAME_MASTER).build();
        Team team = Team.builder().id(teamId).name("Party A").gameMaster(gm).inviteCode("VALIDCOD").build();

        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        when(teamRepository.findByInviteCode("VALIDCOD")).thenReturn(Optional.of(team));
        when(teamMemberRepository.existsById(any(TeamMemberId.class))).thenReturn(false);
        when(teamMemberRepository.save(any())).thenReturn(null);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        TeamResponse expected = TeamResponse.builder().name("Party A").build();
        when(teamMapper.toResponse(team)).thenReturn(expected);

        JoinTeamRequest req = JoinTeamRequest.builder().inviteCode("VALIDCOD").build();
        TeamResponse result = teamService.joinTeam(req, "player1");

        assertEquals("Party A", result.getName());
    }

    @Test
    void joinTeam_invalidCode_throws() {
        User player = User.builder().id(UUID.randomUUID()).username("player1").role(Role.PLAYER).build();
        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        when(teamRepository.findByInviteCode("BADCODE1")).thenReturn(Optional.empty());

        JoinTeamRequest req = JoinTeamRequest.builder().inviteCode("BADCODE1").build();
        assertThrows(ResourceNotFoundException.class,
                () -> teamService.joinTeam(req, "player1"));
    }
}
