package com.dnd.app.controller;

import com.dnd.app.dto.response.*;
import com.dnd.app.service.ReferenceDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reference")
@RequiredArgsConstructor
@Tag(name = "Vanilla Reference Data", description = "System 5e reference data for character templates (no campaign)")
public class VanillaReferenceController {

    private final ReferenceDataService referenceDataService;

    @GetMapping("/classes")
    @Operation(summary = "Get vanilla (system) classes for character templates")
    public ResponseEntity<ApiResponse<List<CharacterClassDetailResponse>>> getClasses() {
        return ResponseEntity.ok(ApiResponse.ok(referenceDataService.getVanillaClasses()));
    }

    @GetMapping("/races")
    @Operation(summary = "Get vanilla (system) races for character templates")
    public ResponseEntity<ApiResponse<List<CharacterRaceDetailResponse>>> getRaces() {
        return ResponseEntity.ok(ApiResponse.ok(referenceDataService.getVanillaRaces()));
    }

    @GetMapping("/backgrounds")
    @Operation(summary = "Get vanilla (system) backgrounds for character templates")
    public ResponseEntity<ApiResponse<List<BackgroundResponse>>> getBackgrounds() {
        return ResponseEntity.ok(ApiResponse.ok(referenceDataService.getVanillaBackgrounds()));
    }

    @GetMapping("/skills")
    @Operation(summary = "Get 18 proficiency skills")
    public ResponseEntity<ApiResponse<List<ProficiencySkillResponse>>> getSkills() {
        return ResponseEntity.ok(ApiResponse.ok(referenceDataService.getVanillaSkills()));
    }

    @GetMapping("/stat-types")
    @Operation(summary = "Get 6 ability score types")
    public ResponseEntity<ApiResponse<List<StatTypeResponse>>> getStatTypes() {
        return ResponseEntity.ok(ApiResponse.ok(referenceDataService.getVanillaStatTypes()));
    }

    @GetMapping("/currencies")
    @Operation(summary = "Get vanilla (system) currency types")
    public ResponseEntity<ApiResponse<List<CurrencyTypeResponse>>> getCurrencies() {
        return ResponseEntity.ok(ApiResponse.ok(referenceDataService.getVanillaCurrencies()));
    }

    @GetMapping("/spells")
    @Operation(summary = "Get vanilla (system) spells with optional filters")
    public ResponseEntity<ApiResponse<List<SpellResponse>>> getSpells(
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) String school) {
        return ResponseEntity.ok(ApiResponse.ok(
                referenceDataService.getVanillaSpells(classId, level, school)));
    }
}
