package com.dnd.app.controller;

import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
@Tag(name = "Campaigns", description = "Campaign management")
public class CampaignController {

    private final CampaignService campaignService;

    @PostMapping
    @Operation(summary = "Create a new campaign")
    public ResponseEntity<ApiResponse<CampaignResponse>> createCampaign(
            @Valid @RequestBody CreateCampaignRequest request, Authentication auth) {
        CampaignResponse response = campaignService.createCampaign(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(response, "Campaign created"));
    }

    @GetMapping
    @Operation(summary = "List my campaigns")
    public ResponseEntity<ApiResponse<List<CampaignResponse>>> listCampaigns(Authentication auth) {
        List<CampaignResponse> campaigns = campaignService.listMyCampaigns(auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(campaigns));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get campaign details")
    public ResponseEntity<ApiResponse<CampaignDetailResponse>> getCampaign(
            @PathVariable UUID id, Authentication auth) {
        CampaignDetailResponse response = campaignService.getCampaignDetail(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update campaign")
    public ResponseEntity<ApiResponse<CampaignResponse>> updateCampaign(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCampaignRequest request, Authentication auth) {
        CampaignResponse response = campaignService.updateCampaign(id, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Campaign updated"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete campaign (creator only)")
    public ResponseEntity<ApiResponse<Void>> deleteCampaign(
            @PathVariable UUID id, Authentication auth) {
        campaignService.deleteCampaign(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Campaign deleted"));
    }

    @PostMapping("/join")
    @Operation(summary = "Join a campaign by invite code")
    public ResponseEntity<ApiResponse<CampaignResponse>> joinCampaign(
            @Valid @RequestBody JoinCampaignRequest request, Authentication auth) {
        CampaignResponse response = campaignService.joinCampaign(request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Joined campaign"));
    }

    @PostMapping("/{id}/leave")
    @Operation(summary = "Leave a campaign")
    public ResponseEntity<ApiResponse<Void>> leaveCampaign(
            @PathVariable UUID id, Authentication auth) {
        campaignService.leaveCampaign(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Left campaign"));
    }

    @PostMapping("/{id}/kick")
    @Operation(summary = "Kick a member (creator only)")
    public ResponseEntity<ApiResponse<Void>> kickMember(
            @PathVariable UUID id,
            @Valid @RequestBody KickMemberRequest request, Authentication auth) {
        campaignService.kickMember(id, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(null, "Member kicked"));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Change campaign status (creator only)")
    public ResponseEntity<ApiResponse<CampaignResponse>> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeCampaignStatusRequest request, Authentication auth) {
        CampaignResponse response = campaignService.changeCampaignStatus(id, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Campaign status updated"));
    }

    @GetMapping("/{id}/invite-code")
    @Operation(summary = "Get invite code")
    public ResponseEntity<ApiResponse<InviteCodeResponse>> getInviteCode(
            @PathVariable UUID id, Authentication auth) {
        InviteCodeResponse response = campaignService.getInviteCode(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/invite-code/regenerate")
    @Operation(summary = "Regenerate invite code (creator only)")
    public ResponseEntity<ApiResponse<InviteCodeResponse>> regenerateInviteCode(
            @PathVariable UUID id, Authentication auth) {
        InviteCodeResponse response = campaignService.regenerateInviteCode(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.ok(response, "Invite code regenerated"));
    }
}
