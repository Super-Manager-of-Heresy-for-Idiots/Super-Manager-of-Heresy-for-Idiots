package com.dnd.app.controller;

import com.dnd.app.dto.request.AddBattleMonstersRequest;
import com.dnd.app.dto.request.ApplyConditionRequest;
import com.dnd.app.dto.request.DeathSaveRequest;
import com.dnd.app.dto.request.AdjustActionEconomyRequest;
import com.dnd.app.dto.request.ApplyCombatantHpRequest;
import com.dnd.app.dto.request.BattleAttackRequest;
import com.dnd.app.dto.request.BattleUseItemRequest;
import com.dnd.app.dto.request.BattleCastSpellRequest;
import com.dnd.app.dto.request.ConcentrationCheckRequest;
import com.dnd.app.dto.request.CreateBattleRequest;
import com.dnd.app.dto.request.InitiativeOrderRequest;
import com.dnd.app.dto.request.JoinBattleRequest;
import com.dnd.app.dto.featurerule.SpellCastResult;
import com.dnd.app.dto.request.SpendActionRequest;
import com.dnd.app.dto.request.UpdateBattleXpRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.BattleActionResultResponse;
import com.dnd.app.dto.response.BattleLogEntryResponse;
import com.dnd.app.dto.response.BattleResponse;
import com.dnd.app.dto.response.CombatantConditionResponse;
import com.dnd.app.dto.response.CombatantTurnResponse;
import com.dnd.app.integration.map.MapSessionCloser;
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
    private final MapSessionCloser mapSessionCloser;

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

    @GetMapping("/{battleId}/log")
    @Operation(summary = "Combat log for the battle, seq-ordered after afterSeq (members; GM_ONLY hidden from players)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<BattleLogEntryResponse>>>> getBattleLog(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @RequestParam(name = "afterSeq", required = false) Long afterSeq,
            @RequestParam(name = "limit", required = false) Integer limit,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<BattleLogEntryResponse> data =
                    battleService.getBattleLog(campaignId, battleId, afterSeq, limit, auth.getName());
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

    @GetMapping("/{battleId}/characters/{characterId}/initiative-bonus")
    @Operation(summary = "Preview a character's initiative bonus (DEX mod + buffs) so the UI can show d20 + bonus live")
    public CompletableFuture<ResponseEntity<ApiResponse<Integer>>> getInitiativeBonus(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID characterId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            int bonus = battleService.getCharacterInitiativeBonus(campaignId, battleId, characterId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(bonus));
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

    @PostMapping("/{battleId}/attack")
    @Operation(summary = "The active combatant attacks a target: manual d20, server resolves hit/crit vs AC and rolls damage")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleActionResultResponse>>> attack(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @Valid @RequestBody BattleAttackRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleActionResultResponse data = battleService.performAttack(campaignId, battleId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Attack resolved"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/use-item")
    @Operation(summary = "The active character uses a carried consumable (e.g. drinks a healing potion)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleActionResultResponse>>> useItem(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @Valid @RequestBody BattleUseItemRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleActionResultResponse data = battleService.performUseItem(campaignId, battleId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Item used"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/combatants/{combatantId}/spend")
    @Operation(summary = "Mark a combatant's action / bonus action / reaction as spent this turn")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> spendAction(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId,
            @Valid @RequestBody SpendActionRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.spendAction(campaignId, battleId, combatantId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Action spent"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/combatants/{combatantId}/action-economy")
    @Operation(summary = "Adjust a combatant's action / bonus / legendary action maxima — GM only")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> adjustActionEconomy(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId,
            @Valid @RequestBody AdjustActionEconomyRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.adjustActionEconomy(campaignId, battleId, combatantId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Action economy updated"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/combatants/{combatantId}/hp")
    @Operation(summary = "Adjust a combatant's HP (negative damages, positive heals) — GM only")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> applyCombatantHp(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId,
            @Valid @RequestBody ApplyCombatantHpRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.applyCombatantHp(campaignId, battleId, combatantId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "HP updated"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/end")
    @Operation(summary = "End the battle (GM only)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> endBattle(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.endBattle(campaignId, battleId, auth.getName());
            // After the battle transaction has committed, best-effort close any linked map sessions.
            mapSessionCloser.closeSessionsForBattle(battleId);
            return ResponseEntity.ok(ApiResponse.ok(data, "Battle ended"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/combatants/{combatantId}/conditions")
    @Operation(summary = "Apply a condition to a combatant (GM, or the character's owner)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<CombatantConditionResponse>>>> addCondition(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId,
            @Valid @RequestBody ApplyConditionRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<CombatantConditionResponse> data =
                    battleService.applyCondition(campaignId, battleId, combatantId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Condition applied"));
        }, controllerTaskExecutor);
    }

    @DeleteMapping("/{battleId}/combatants/{combatantId}/conditions/{conditionId}")
    @Operation(summary = "Remove a condition from a combatant (GM, or the character's owner)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<CombatantConditionResponse>>>> removeCondition(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId,
            @PathVariable UUID conditionId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<CombatantConditionResponse> data =
                    battleService.removeCondition(campaignId, battleId, combatantId, conditionId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Condition removed"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/combatants/{combatantId}/death-save")
    @Operation(summary = "Roll a death saving throw for a dying character (server d20 or manual)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> deathSave(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId,
            @Valid @RequestBody(required = false) DeathSaveRequest request, Authentication auth) {
        Integer roll = request != null ? request.getRoll() : null;
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.rollDeathSave(campaignId, battleId, combatantId, roll, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Death save rolled"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/combatants/{combatantId}/concentration-check")
    @Operation(summary = "Roll a pending concentration saving throw (player d20 or server AUTO)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> concentrationCheck(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId,
            @Valid @RequestBody(required = false) ConcentrationCheckRequest request, Authentication auth) {
        Integer d20 = request != null ? request.getD20() : null;
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.resolveConcentration(campaignId, battleId, combatantId, d20, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Concentration check resolved"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/combatants/{combatantId}/stabilize")
    @Operation(summary = "Stabilize a dying character (GM/healer)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> stabilize(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.stabilize(campaignId, battleId, combatantId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Stabilized"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/cast-spell")
    @Operation(summary = "Cast a spell on the caster's turn via the feature-rules runtime (Phase 2.1)")
    public CompletableFuture<ResponseEntity<ApiResponse<SpellCastResult>>> castSpell(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @Valid @RequestBody BattleCastSpellRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            SpellCastResult data = battleService.castSpell(campaignId, battleId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Spell cast"));
        }, controllerTaskExecutor);
    }

    @PatchMapping("/{battleId}/initiative-order")
    @Operation(summary = "Replace the whole tracker's initiative values (GM drag-reorder quick tool)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> setInitiativeOrder(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @Valid @RequestBody InitiativeOrderRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.setInitiativeOrder(
                    campaignId, battleId, request.getEntries(), auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Initiative order updated"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/combatants/{combatantId}/reroll-initiative")
    @Operation(summary = "Reroll a combatant's initiative and re-sort the tracker (GM quick tool)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> rerollInitiative(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.rerollInitiative(campaignId, battleId, combatantId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Initiative rerolled"));
        }, controllerTaskExecutor);
    }
}
