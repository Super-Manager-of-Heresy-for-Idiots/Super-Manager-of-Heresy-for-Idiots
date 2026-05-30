package com.dnd.app.controller;

import com.dnd.app.dto.request.ActivateHomebrewRequest;
import com.dnd.app.dto.request.CreateTeamRequest;
import com.dnd.app.dto.request.JoinTeamRequest;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.TeamContentService;
import com.dnd.app.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamController {

    private final TeamService teamService;
    private final TeamContentService teamContentService;

    @PostMapping
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(
            @Valid @RequestBody CreateTeamRequest request, Authentication auth) {
        TeamResponse team = teamService.createTeam(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(team, "Команда создана"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TeamResponse>>> listTeams(Authentication auth) {
        List<TeamResponse> teams = teamService.listTeams(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(teams));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamResponse>> getTeam(@PathVariable UUID id, Authentication auth) {
        TeamResponse team = teamService.getTeamById(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(team));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TeamResponse>> updateTeam(
            @PathVariable UUID id, @Valid @RequestBody CreateTeamRequest request, Authentication auth) {
        TeamResponse team = teamService.updateTeam(id, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(team, "Команда обновлена"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTeam(@PathVariable UUID id, Authentication auth) {
        teamService.deleteTeam(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Команда удалена"));
    }

    @PostMapping("/{id}/regenerate-invite")
    public ResponseEntity<ApiResponse<InviteCodeResponse>> regenerateInvite(
            @PathVariable UUID id, Authentication auth) {
        InviteCodeResponse code = teamService.regenerateInviteCode(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(code, "Код приглашения обновлен"));
    }

    @GetMapping("/{id}/invite-code")
    public ResponseEntity<ApiResponse<InviteCodeResponse>> getInviteCode(
            @PathVariable UUID id, Authentication auth) {
        InviteCodeResponse code = teamService.getInviteCode(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(code));
    }

    @PostMapping("/join")
    public ResponseEntity<ApiResponse<TeamResponse>> joinTeam(
            @Valid @RequestBody JoinTeamRequest request, Authentication auth) {
        TeamResponse team = teamService.joinTeam(request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(team, "Вы вступили в команду"));
    }

    @PostMapping("/{teamId}/homebrew")
    public ResponseEntity<ApiResponse<TeamHomebrewActivationResponse>> activateHomebrew(
            @PathVariable UUID teamId, @Valid @RequestBody ActivateHomebrewRequest request, Authentication auth) {
        TeamHomebrewActivationResponse resp = teamContentService.activateHomebrew(teamId, request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(resp, "Хомбрю-пакет активирован для команды"));
    }

    @DeleteMapping("/{teamId}/homebrew/{packageId}")
    public ResponseEntity<ApiResponse<Void>> deactivateHomebrew(
            @PathVariable UUID teamId, @PathVariable UUID packageId, Authentication auth) {
        teamContentService.deactivateHomebrew(teamId, packageId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Хомбрю-пакет деактивирован для команды"));
    }

    @GetMapping("/{teamId}/homebrew")
    public ResponseEntity<ApiResponse<List<TeamHomebrewActivationResponse>>> listActiveHomebrew(
            @PathVariable UUID teamId, Authentication auth) {
        List<TeamHomebrewActivationResponse> list = teamContentService.listActiveHomebrew(teamId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{teamId}/available-content")
    public ResponseEntity<ApiResponse<TeamAvailableContentResponse>> getAvailableContent(
            @PathVariable UUID teamId, Authentication auth) {
        TeamAvailableContentResponse content = teamContentService.getAvailableContent(teamId, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(content));
    }
}
