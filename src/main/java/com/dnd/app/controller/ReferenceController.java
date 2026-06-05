package com.dnd.app.controller;

import com.dnd.app.dto.response.*;
import com.dnd.app.service.ReferenceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns/{campaignId}/reference")
@RequiredArgsConstructor
@Tag(name = "Reference Data", description = "5e reference data for character wizard")
public class ReferenceController {

    private final ReferenceDataService referenceDataService;

    @GetMapping("/classes")
    @Operation(summary = "Get available classes with 5e metadata")
    public ResponseEntity<ApiResponse<List<CharacterClassDetailResponse>>> getClasses(
            @PathVariable UUID campaignId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                referenceDataService.getClasses(campaignId, auth.getName())));
    }

    @GetMapping("/races")
    @Operation(summary = "Get available races with subraces")
    public ResponseEntity<ApiResponse<List<CharacterRaceDetailResponse>>> getRaces(
            @PathVariable UUID campaignId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                referenceDataService.getRaces(campaignId, auth.getName())));
    }

    @GetMapping("/backgrounds")
    @Operation(summary = "Get available backgrounds")
    public ResponseEntity<ApiResponse<List<BackgroundResponse>>> getBackgrounds(
            @PathVariable UUID campaignId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                referenceDataService.getBackgrounds(campaignId, auth.getName())));
    }

    @GetMapping("/skills")
    @Operation(summary = "Get 18 proficiency skills")
    public ResponseEntity<ApiResponse<List<ProficiencySkillResponse>>> getSkills(
            @PathVariable UUID campaignId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                referenceDataService.getSkills(campaignId, auth.getName())));
    }

    @GetMapping("/stat-types")
    @Operation(summary = "Get 6 ability score types")
    public ResponseEntity<ApiResponse<List<StatTypeResponse>>> getStatTypes(
            @PathVariable UUID campaignId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                referenceDataService.getStatTypes(campaignId, auth.getName())));
    }

    @GetMapping("/currencies")
    @Operation(summary = "Get available currency types")
    public ResponseEntity<ApiResponse<List<CurrencyTypeResponse>>> getCurrencies(
            @PathVariable UUID campaignId, Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                referenceDataService.getCurrencies(campaignId, auth.getName())));
    }

    @GetMapping("/spells")
    @Operation(summary = "Get spells with optional filters")
    public ResponseEntity<ApiResponse<List<SpellResponse>>> getSpells(
            @PathVariable UUID campaignId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) String school,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(
                referenceDataService.getSpells(campaignId, auth.getName(), classId, level, school)));
    }
}
