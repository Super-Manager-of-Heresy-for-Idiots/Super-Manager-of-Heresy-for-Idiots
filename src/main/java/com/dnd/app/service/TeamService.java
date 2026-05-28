package com.dnd.app.service;

import com.dnd.app.domain.Team;
import com.dnd.app.domain.TeamMember;
import com.dnd.app.domain.TeamMemberId;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.CreateTeamRequest;
import com.dnd.app.dto.request.JoinTeamRequest;
import com.dnd.app.dto.response.InviteCodeResponse;
import com.dnd.app.dto.response.TeamResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.TeamMapper;
import com.dnd.app.repository.TeamMemberRepository;
import com.dnd.app.repository.TeamRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.util.InviteCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final TeamMapper teamMapper;

    @Transactional
    public TeamResponse createTeam(CreateTeamRequest request, String username) {
        User gm = getUser(username);
        if (gm.getRole() != Role.GAME_MASTER && gm.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только мастера игры могут создавать команды");
        }
        Team team = Team.builder()
                .name(request.getName())
                .gameMaster(gm)
                .inviteCode(InviteCodeGenerator.generate())
                .build();
        team = teamRepository.save(team);
        log.info("Team created: id={}, name='{}', gm={}", team.getId(), team.getName(), username);
        return teamMapper.toResponse(team);
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> listTeams(String username) {
        User user = getUser(username);
        List<Team> teams;
        if (user.getRole() == Role.ADMIN) {
            teams = teamRepository.findAll();
        } else if (user.getRole() == Role.GAME_MASTER) {
            teams = teamRepository.findAllByGameMasterId(user.getId());
        } else {
            throw new AccessDeniedException("Игроки не могут просматривать список команд");
        }
        return teams.stream().map(teamMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TeamResponse getTeamById(UUID id, String username) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Команда не найдена"));
        User user = getUser(username);
        if (user.getRole() == Role.GAME_MASTER && !team.getGameMaster().getId().equals(user.getId())) {
            throw new AccessDeniedException("Эта команда вам не принадлежит");
        }
        if (user.getRole() == Role.PLAYER) {
            throw new AccessDeniedException("Игроки не могут напрямую просматривать детали команды");
        }
        return teamMapper.toResponse(team);
    }

    @Transactional
    public TeamResponse updateTeam(UUID id, CreateTeamRequest request, String username) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Команда не найдена"));
        User user = getUser(username);
        boolean isTeamOwner = user.getRole() == Role.GAME_MASTER && team.getGameMaster().getId().equals(user.getId());
        if (!isTeamOwner && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только владелец-мастер может обновлять эту команду");
        }
        team.setName(request.getName());
        team = teamRepository.save(team);
        return teamMapper.toResponse(team);
    }

    @Transactional
    public void deleteTeam(UUID id, String username) {
        Team team = teamRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Команда не найдена"));
        User user = getUser(username);
        boolean isTeamOwner = user.getRole() == Role.GAME_MASTER && team.getGameMaster().getId().equals(user.getId());
        if (!isTeamOwner && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только владелец-мастер может удалить эту команду");
        }
        log.info("Team deleted: id={}, name='{}', by user={}", id, team.getName(), username);
        teamRepository.delete(team);
    }

    @Transactional
    public InviteCodeResponse regenerateInviteCode(UUID teamId, String username) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Команда не найдена"));
        User user = getUser(username);
        boolean isTeamOwner = user.getRole() == Role.GAME_MASTER && team.getGameMaster().getId().equals(user.getId());
        if (!isTeamOwner && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только владелец-мастер может обновлять коды приглашения");
        }
        team.setInviteCode(InviteCodeGenerator.generate());
        teamRepository.save(team);
        return InviteCodeResponse.builder().inviteCode(team.getInviteCode()).build();
    }

    @Transactional(readOnly = true)
    public InviteCodeResponse getInviteCode(UUID teamId, String username) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new ResourceNotFoundException("Команда не найдена"));
        User user = getUser(username);
        boolean isTeamOwner = user.getRole() == Role.GAME_MASTER && team.getGameMaster().getId().equals(user.getId());
        if (!isTeamOwner && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только владелец-мастер может просматривать коды приглашения");
        }
        return InviteCodeResponse.builder().inviteCode(team.getInviteCode()).build();
    }

    @Transactional
    public TeamResponse joinTeam(JoinTeamRequest request, String username) {
        User player = getUser(username);
        if (player.getRole() != Role.PLAYER) {
            throw new AccessDeniedException("Только игроки могут вступать в команды");
        }
        Team team = teamRepository.findByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new ResourceNotFoundException("Некорректный код приглашения"));
        TeamMemberId memberId = new TeamMemberId(team.getId(), player.getId());
        if (teamMemberRepository.existsById(memberId)) {
            throw new DuplicateResourceException("Вы уже состоите в этой команде");
        }
        TeamMember member = TeamMember.builder()
                .id(memberId)
                .team(team)
                .player(player)
                .build();
        teamMemberRepository.save(member);
        log.info("Player joined team: player={}, teamId={}, teamName='{}'", username, team.getId(), team.getName());
        team = teamRepository.findById(team.getId()).orElseThrow();
        return teamMapper.toResponse(team);
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }
}
