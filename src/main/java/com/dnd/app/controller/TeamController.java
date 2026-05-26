package com.dnd.app.controller;

import com.dnd.app.dto.request.CreateTeamRequest;
import com.dnd.app.dto.request.JoinTeamRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.InviteCodeResponse;
import com.dnd.app.dto.response.TeamResponse;
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

    @PostMapping
    public ResponseEntity<ApiResponse<TeamResponse>> createTeam(
            @Valid @RequestBody CreateTeamRequest request, Authentication auth) {
        TeamResponse team = teamService.createTeam(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(team, "Team created"));
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
        return ResponseEntity.ok(ApiResponse.ok(team, "Team updated"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTeam(@PathVariable UUID id, Authentication auth) {
        teamService.deleteTeam(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Team deleted"));
    }

    @PostMapping("/{id}/regenerate-invite")
    public ResponseEntity<ApiResponse<InviteCodeResponse>> regenerateInvite(
            @PathVariable UUID id, Authentication auth) {
        InviteCodeResponse code = teamService.regenerateInviteCode(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(code, "Invite code regenerated"));
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
        return ResponseEntity.ok(ApiResponse.ok(team, "Joined team successfully"));
    }
}
