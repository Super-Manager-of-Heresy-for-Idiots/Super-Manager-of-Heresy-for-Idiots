package com.dnd.app.controller;

import com.dnd.app.dto.request.MonsterRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.MonsterResponse;
import com.dnd.app.dto.response.MonsterSummaryResponse;
import com.dnd.app.service.MonsterService;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Campaign-scoped monsters. The campaign GM (master) creates monsters or clones existing
 * ones into the campaign, hides them until ready, reveals them to players, and removes
 * them once defeated. Players see only monsters marked visible. Checks live in the service.
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/monsters")
@RequiredArgsConstructor
@Tag(name = "Campaign Monsters", description = "Campaign-scoped monster management")
public class CampaignMonsterController {

    private final MonsterService monsterService;
    private final Executor controllerTaskExecutor;

    @GetMapping
    @Operation(summary = "List campaign monsters (GM sees all, players see visible only)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<MonsterSummaryResponse>>>> listMonsters(
            @PathVariable UUID campaignId, Authentication auth,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() -> {
            List<MonsterSummaryResponse> data = monsterService.listCampaignMonsters(campaignId, auth.getName(), lang);
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get monster details")
    public CompletableFuture<ResponseEntity<ApiResponse<MonsterResponse>>> getMonster(
            @PathVariable UUID campaignId,
            @PathVariable UUID id, Authentication auth,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() -> {
            MonsterResponse data = monsterService.getMonster(id, auth.getName(), lang);
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    @PostMapping
    @Operation(summary = "Create a campaign monster (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<MonsterResponse>>> createMonster(
            @PathVariable UUID campaignId,
            @Valid @RequestBody MonsterRequest request, Authentication auth,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() -> {
            MonsterResponse data = monsterService.createCampaignMonster(campaignId, request, auth.getName(), lang);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data, "Monster created"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/clone/{sourceId}")
    @Operation(summary = "Clone a system, homebrew, or campaign monster into this campaign (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<MonsterResponse>>> cloneMonster(
            @PathVariable UUID campaignId,
            @PathVariable UUID sourceId, Authentication auth,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() -> {
            MonsterResponse data = monsterService.cloneMonsterIntoCampaign(campaignId, sourceId, auth.getName(), lang);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data, "Monster cloned"));
        }, controllerTaskExecutor);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a campaign monster (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<MonsterResponse>>> updateMonster(
            @PathVariable UUID campaignId,
            @PathVariable UUID id,
            @Valid @RequestBody MonsterRequest request, Authentication auth,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() -> {
            MonsterResponse data = monsterService.updateCampaignMonster(id, request, auth.getName(), lang);
            return ResponseEntity.ok(ApiResponse.ok(data, "Monster updated"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{id}/toggle-visibility")
    @Operation(summary = "Reveal or hide a campaign monster from players (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<MonsterResponse>>> toggleVisibility(
            @PathVariable UUID campaignId,
            @PathVariable UUID id, Authentication auth,
            @RequestParam(defaultValue = "en") String lang) {
        return CompletableFuture.supplyAsync(() -> {
            MonsterResponse data = monsterService.toggleCampaignMonsterVisibility(id, auth.getName(), lang);
            return ResponseEntity.ok(ApiResponse.ok(data, "Visibility toggled"));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a campaign monster (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> deleteMonster(
            @PathVariable UUID campaignId,
            @PathVariable UUID id, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            monsterService.deleteCampaignMonster(id, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(null, "Monster deleted"));
        }, controllerTaskExecutor);
    }
}
