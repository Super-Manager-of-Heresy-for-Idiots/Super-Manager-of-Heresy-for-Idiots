package com.dnd.app.controller;

import com.dnd.app.dto.request.AddBattleMonstersRequest;
import com.dnd.app.dto.request.CreateBattleRequest;
import com.dnd.app.dto.request.JoinBattleRequest;
import com.dnd.app.dto.request.UpdateBattleXpRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.BattleResponse;
import com.dnd.app.dto.response.CombatantTurnResponse;
import com.dnd.app.service.BattleService;
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
 * Campaign-scoped battles. The GM assembles a monster group (with a danger / xp preview),
 * starts the fight, players join their characters with a d20 roll, and the shared initiative
 * tracker drives turn passing. Every state change is broadcast to the campaign topic so all
 * participants re-fetch the authoritative state. Authorization lives in the service.
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/battles")
@RequiredArgsConstructor
@Tag(name = "Battles", description = "Campaign battle lifecycle, group assembly and turn flow")
public class BattleController {

    private final BattleService battleService;
    private final Executor controllerTaskExecutor;

    @PostMapping
    @Operation(summary = "Create a battle in the assembling state (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> createBattle(
            @PathVariable UUID campaignId,
            @Valid @RequestBody(required = false) CreateBattleRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.createBattle(campaignId, request, auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(data, "Battle created"));
        }, controllerTaskExecutor);
    }

    @GetMapping
    @Operation(summary = "List battles in the campaign (members)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<BattleResponse>>>> listBattles(
            @PathVariable UUID campaignId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<BattleResponse> data = battleService.listBattles(campaignId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    @GetMapping("/{battleId}")
    @Operation(summary = "Get full battle state with the initiative tracker (members)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> getBattle(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.getBattle(campaignId, battleId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/monsters")
    @Operation(summary = "Add monsters to the group while assembling (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> addMonsters(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @Valid @RequestBody AddBattleMonstersRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.addMonsters(campaignId, battleId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Monsters added"));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/{battleId}/combatants/{combatantId}")
    @Operation(summary = "Remove a combatant while assembling (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> removeCombatant(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.removeCombatant(campaignId, battleId, combatantId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Combatant removed"));
        }, controllerTaskExecutor);
    }

    @PutMapping("/{battleId}/xp")
    @Operation(summary = "Override the group's total combat XP (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> setOverrideXp(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @Valid @RequestBody UpdateBattleXpRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.setOverrideXp(campaignId, battleId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Battle XP updated"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/start")
    @Operation(summary = "Roll monster initiative, activate the battle and notify players (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> startBattle(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.startBattle(campaignId, battleId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Battle started"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/join")
    @Operation(summary = "Join one or more of your characters with a d20 (manual or server-rolled)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> joinCharacters(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @Valid @RequestBody JoinBattleRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.joinCharacters(campaignId, battleId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Characters joined"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/end-turn")
    @Operation(summary = "Pass the turn to the next combatant (GM or the active character's owner)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> endTurn(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.endTurn(campaignId, battleId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Turn passed"));
        }, controllerTaskExecutor);
    }

    @GetMapping("/{battleId}/current-turn")
    @Operation(summary = "Get the active combatant's detail (character sheet + resources, or monster for the GM)")
    public CompletableFuture<ResponseEntity<ApiResponse<CombatantTurnResponse>>> getCurrentTurn(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CombatantTurnResponse data = battleService.getCurrentTurn(campaignId, battleId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/end")
    @Operation(summary = "End the battle (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<Void>>> endBattle(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            battleService.endBattle(campaignId, battleId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok((Void) null, "Battle ended"));
        }, controllerTaskExecutor);
    }
}
