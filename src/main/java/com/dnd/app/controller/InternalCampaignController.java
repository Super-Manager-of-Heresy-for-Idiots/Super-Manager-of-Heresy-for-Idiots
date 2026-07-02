package com.dnd.app.controller;

import com.dnd.app.dto.response.CampaignAccessResponse;
import com.dnd.app.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Service-to-service campaign authorization projections for map-service.
 *
 * <p>Secured by {@code X-Internal-Api-Key}; callers do not present a user JWT.
 */
@RestController
@RequestMapping("/api/internal/campaigns/{campaignId}")
@RequiredArgsConstructor
@Tag(name = "Internal Campaign", description = "Service-to-service campaign access checks")
public class InternalCampaignController {

    private final CampaignService campaignService;

    @GetMapping("/access")
    @Operation(summary = "What a user may do with campaign maps")
    public ResponseEntity<CampaignAccessResponse> getCampaignAccess(
            @PathVariable UUID campaignId,
            @RequestParam UUID userId) {
        return ResponseEntity.ok(campaignService.getCampaignAccess(campaignId, userId));
    }
}
