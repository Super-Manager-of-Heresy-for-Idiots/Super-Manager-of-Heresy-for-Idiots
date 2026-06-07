package com.dnd.app.controller;

import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.CampaignContentService;
import com.dnd.app.service.CampaignService;
import com.dnd.app.service.RaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Tag(name = "Campaigns", description = "Campaign management")
public class CampaignController {

    private final CampaignService campaignService;
    private final CampaignContentService campaignContentService;
    private final RaceService raceService;
    private final Executor controllerTaskExecutor;

    @PostMapping
    @Operation(summary = "Create a new campaign")
    public CompletableFuture<ResponseEntity<ApiResponse<CampaignResponse>>> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampaignResponse response = campaignService.createCampaign(request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(response, "Campaign created"));
        }, controllerTaskExecutor);
    }

    @GetMapping
    @Operation(summary = "List my campaigns (paginated)")
    public CompletableFuture<ResponseEntity<ApiResponse<Page<CampaignResponse>>>> listCampaigns(
            Pageable pageable, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            Page<CampaignResponse> campaigns = campaignService.listMyCampaigns(auth.getName(), pageable);
            return ResponseEntity.ok(ApiResponse.ok(campaigns));
        }, controllerTaskExecutor);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get campaign details")
    public CompletableFuture<ResponseEntity<ApiResponse<CampaignDetailResponse>>> getCampaign(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampaignDetailResponse response = campaignService.getCampaignDetail(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response));
        }, controllerTaskExecutor);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update campaign")
    public CompletableFuture<ResponseEntity<ApiResponse<CampaignResponse>>> updateCampaign(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCampaignRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampaignResponse response = campaignService.updateCampaign(id, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Campaign updated"));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete campaign (creator only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteCampaign(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            campaignService.deleteCampaign(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Campaign deleted"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/join")
    @Operation(summary = "Join a campaign by invite code")
    public CompletableFuture<ResponseEntity<ApiResponse<CampaignResponse>>> joinCampaign(
            @Valid @RequestBody JoinCampaignRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampaignResponse response = campaignService.joinCampaign(request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Joined campaign"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{id}/leave")
    @Operation(summary = "Leave a campaign")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> leaveCampaign(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            campaignService.leaveCampaign(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Left campaign"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{id}/kick")
    @Operation(summary = "Kick a member (creator only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> kickMember(
            @PathVariable UUID id,
            @Valid @RequestBody KickMemberRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            campaignService.kickMember(id, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Member kicked"));
        }, controllerTaskExecutor);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Change campaign status (creator only)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampaignResponse>>> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeCampaignStatusRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampaignResponse response = campaignService.changeCampaignStatus(id, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Campaign status updated"));
        }, controllerTaskExecutor);
    }

    @GetMapping("/{id}/invite-code")
    @Operation(summary = "Get invite code")
    public CompletableFuture<ResponseEntity<ApiResponse<InviteCodeResponse>>> getInviteCode(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            InviteCodeResponse response = campaignService.getInviteCode(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{id}/invite-code/regenerate")
    @Operation(summary = "Regenerate invite code (creator only)")
    public CompletableFuture<ResponseEntity<ApiResponse<InviteCodeResponse>>> regenerateInviteCode(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            InviteCodeResponse response = campaignService.regenerateInviteCode(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(response, "Invite code regenerated"));
        }, controllerTaskExecutor);
    }

    // --- Character reassignment ---

    @PostMapping("/{id}/characters/{characterId}/reassign")
    @Operation(summary = "Reassign RESERVE character to new owner with deep copy (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<CharacterResponse>>> reassignCharacter(
            @PathVariable UUID id,
            @PathVariable UUID characterId,
            @Valid @RequestBody ReassignCharacterRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CharacterResponse response = campaignService.reassignCharacter(id, characterId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(response, "Character reassigned to new owner"));
        }, controllerTaskExecutor);
    }

    // --- Homebrew ---

    @PostMapping("/{id}/homebrew")
    @Operation(summary = "Attach homebrew package to campaign (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampaignHomebrewResponse>>> activateHomebrew(
            @PathVariable UUID id,
            @Valid @RequestBody ActivateHomebrewRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampaignHomebrewResponse resp = campaignContentService.activateHomebrew(id, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(resp, "Хомбрю-пакет активирован для кампании"));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/{id}/homebrew/{packageId}")
    @Operation(summary = "Detach homebrew package from campaign (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deactivateHomebrew(
            @PathVariable UUID id, @PathVariable UUID packageId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            campaignContentService.deactivateHomebrew(id, packageId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Хомбрю-пакет деактивирован для кампании"));
        }, controllerTaskExecutor);
    }

    @PutMapping("/{id}/homebrew/{packageId}/version")
    @Operation(summary = "Update pinned homebrew version for campaign (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampaignHomebrewResponse>>> updatePinnedVersion(
            @PathVariable UUID id, @PathVariable UUID packageId,
            @Valid @RequestBody com.dnd.app.dto.request.UpdatePinnedVersionRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampaignHomebrewResponse resp = campaignContentService.updatePinnedVersion(id, packageId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(resp, "Pinned version updated"));
        }, controllerTaskExecutor);
    }

    @GetMapping("/{id}/homebrew")
    @Operation(summary = "List active homebrew packages in campaign")
    public CompletableFuture<ResponseEntity<ApiResponse<List<CampaignHomebrewResponse>>>> listActiveHomebrew(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<CampaignHomebrewResponse> list = campaignContentService.listActiveHomebrew(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(list));
        }, controllerTaskExecutor);
    }

    @GetMapping("/{id}/available-content")
    @Operation(summary = "Get available content for campaign (global + homebrew)")
    public CompletableFuture<ResponseEntity<ApiResponse<CampaignAvailableContentResponse>>> getAvailableContent(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CampaignAvailableContentResponse content = campaignContentService.getAvailableContent(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(content));
        }, controllerTaskExecutor);
    }

    @GetMapping("/{id}/races")
    @Operation(summary = "List races available for character creation in campaign")
    public CompletableFuture<ResponseEntity<ApiResponse<List<RaceListItemResponse>>>> listAvailableRaces(
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(raceService.listAvailableForCampaign(id, auth.getName()))),
                controllerTaskExecutor);
    }

    @GetMapping("/{id}/races/{raceId}")
    @Operation(summary = "Get race details available in campaign")
    public CompletableFuture<ResponseEntity<ApiResponse<RaceResponse>>> getAvailableRace(
            @PathVariable UUID id, @PathVariable UUID raceId, Authentication auth) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(raceService.getAvailableRace(id, raceId, auth.getName()))),
                controllerTaskExecutor);
    }
}
