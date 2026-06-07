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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/reference")
@RequiredArgsConstructor
@Tag(name = "Vanilla Reference Data", description = "System 5e reference data for character templates (no campaign)")
public class VanillaReferenceController {

    private final ReferenceDataService referenceDataService;
    private final Executor controllerTaskExecutor;

    @GetMapping("/classes")
    @Operation(summary = "Get vanilla (system) classes for character templates")
    public CompletableFuture<ResponseEntity<ApiResponse<List<CharacterClassDetailResponse>>>> getClasses() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getVanillaClasses())),
                controllerTaskExecutor);
    }

    @GetMapping("/races")
    @Operation(summary = "Get vanilla (system) races for character templates")
    public CompletableFuture<ResponseEntity<ApiResponse<List<CharacterRaceDetailResponse>>>> getRaces() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getVanillaRaces())),
                controllerTaskExecutor);
    }

    @GetMapping("/backgrounds")
    @Operation(summary = "Get vanilla (system) backgrounds for character templates")
    public CompletableFuture<ResponseEntity<ApiResponse<List<BackgroundResponse>>>> getBackgrounds() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getVanillaBackgrounds())),
                controllerTaskExecutor);
    }

    @GetMapping("/skills")
    @Operation(summary = "Get 18 proficiency skills")
    public CompletableFuture<ResponseEntity<ApiResponse<List<ProficiencySkillResponse>>>> getSkills() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getVanillaSkills())),
                controllerTaskExecutor);
    }

    @GetMapping("/stat-types")
    @Operation(summary = "Get 6 ability score types")
    public CompletableFuture<ResponseEntity<ApiResponse<List<StatTypeResponse>>>> getStatTypes() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getVanillaStatTypes())),
                controllerTaskExecutor);
    }

    @GetMapping("/currencies")
    @Operation(summary = "Get vanilla (system) currency types")
    public CompletableFuture<ResponseEntity<ApiResponse<List<CurrencyTypeResponse>>>> getCurrencies() {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(referenceDataService.getVanillaCurrencies())),
                controllerTaskExecutor);
    }

    @GetMapping("/spells")
    @Operation(summary = "Get vanilla (system) spells with optional filters")
    public CompletableFuture<ResponseEntity<ApiResponse<List<SpellResponse>>>> getSpells(
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) String school) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.ok(ApiResponse.ok(
                        referenceDataService.getVanillaSpells(classId, level, school))),
                controllerTaskExecutor);
    }
}
