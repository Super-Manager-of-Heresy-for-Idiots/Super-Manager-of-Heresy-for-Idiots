package com.dnd.app.controller;

import com.dnd.app.dto.request.AddBattleMonstersRequest;
import com.dnd.app.dto.request.ApplyConditionRequest;
import com.dnd.app.dto.request.DeathSaveRequest;
import com.dnd.app.dto.request.AdjustActionEconomyRequest;
import com.dnd.app.dto.request.ApplyCombatantHpRequest;
import com.dnd.app.dto.request.BattleAttackRequest;
import com.dnd.app.dto.request.BattleUseItemRequest;
import com.dnd.app.dto.request.BattleCastSpellRequest;
import com.dnd.app.dto.request.BulkActionRequest;
import com.dnd.app.dto.request.ConcentrationCheckRequest;
import com.dnd.app.dto.request.GroupInitiativeRequest;
import com.dnd.app.dto.request.CreateBattleRequest;
import com.dnd.app.dto.request.InitiativeOrderRequest;
import com.dnd.app.dto.request.JoinBattleRequest;
import com.dnd.app.dto.featurerule.SpellCastResult;
import com.dnd.app.dto.request.SpendActionRequest;
import com.dnd.app.dto.request.StandardActionRequest;
import com.dnd.app.dto.request.ContestRequest;
import com.dnd.app.dto.request.ForcedMoveRequest;
import com.dnd.app.dto.request.TeleportRequest;
import com.dnd.app.dto.request.FallRequest;
import com.dnd.app.dto.request.ReadyActionRequest;
import com.dnd.app.dto.request.TrapTriggerRequest;
import com.dnd.app.dto.request.UpdateBattleXpRequest;
import com.dnd.app.dto.response.ApiResponse;
import com.dnd.app.dto.response.BattleActionResultResponse;
import com.dnd.app.dto.response.BattleLogEntryResponse;
import com.dnd.app.dto.response.BattleResponse;
import com.dnd.app.dto.response.ContestResultResponse;
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
 * Класс BattleController описывает REST-контроллер, который связывает HTTP-запросы с бизнес-сценариями приложения.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@RestController
@RequestMapping("/api/campaigns/{campaignId}/battles")
@RequiredArgsConstructor
@Tag(name = "Battles", description = "Campaign battle lifecycle, group assembly and turn flow")
public class BattleController {

    private final BattleService battleService;
    private final Executor controllerTaskExecutor;
    private final MapSessionCloser mapSessionCloser;

    /**
     * Создает результат операции "create battle" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Возвращает список для операции "list battles" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @GetMapping
    @Operation(summary = "List battles in the campaign (members)")
    public CompletableFuture<ResponseEntity<ApiResponse<List<BattleResponse>>>> listBattles(
            @PathVariable UUID campaignId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            List<BattleResponse> data = battleService.listBattles(campaignId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get battle" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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
    /**
     * Возвращает результат операции "get battle log" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param afterSeq граница выборки, используемая для продолжения бизнес-потока
     * @param limit ограничение размера результата бизнес-операции
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Добавляет результат операции "add monsters" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Удаляет результат операции "remove combatant" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Устанавливает результат операции "set override xp" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Выполняет операции "start battle" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Выполняет операции "join characters" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Возвращает результат операции "get initiative bonus" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Выполняет операции "end turn" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    /**
     * Передаёт ход следующему комбатанту; опциональные параметры дают защиту realtime (фаза 2.14):
     * дедуп по {@code clientCommandId} и защиту от двойного next-turn по {@code expectedTurnIndex}/
     * {@code expectedRound}.
     *
     * @param campaignId        идентификатор кампании
     * @param battleId          идентификатор боя
     * @param expectedTurnIndex ожидаемый индекс хода (опц.)
     * @param expectedRound     ожидаемый номер раунда (опц.)
     * @param clientCommandId   идемпотентный ключ команды (опц.)
     * @param auth              аутентификация инициатора
     * @return обёрнутое актуальное состояние боя
     */
    @PostMapping("/{battleId}/end-turn")
    @Operation(summary = "Pass the turn to the next combatant (GM or the active character's owner)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> endTurn(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @RequestParam(required = false) Integer expectedTurnIndex,
            @RequestParam(required = false) Integer expectedRound,
            @RequestParam(required = false) UUID clientCommandId,
            Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.endTurn(campaignId, battleId,
                    expectedTurnIndex, expectedRound, clientCommandId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Turn passed"));
        }, controllerTaskExecutor);
    }

    /**
     * Возвращает результат операции "get current turn" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    @GetMapping("/{battleId}/combatants/{combatantId}/turn")
    @Operation(summary = "Get any combatant's actionable detail (attacks) — for off-turn reaction / OA resolution")
    public CompletableFuture<ResponseEntity<ApiResponse<CombatantTurnResponse>>> getCombatantTurn(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            CombatantTurnResponse data = battleService.getCombatantTurn(campaignId, battleId, combatantId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "attack" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Выполняет операции "use item" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Выполняет операции "spend action" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Выполняет операции "standard action" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{battleId}/combatants/{combatantId}/standard-action")
    @Operation(summary = "Take a standard action: Dash / Dodge / Disengage / Help / Hide (own turn)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> standardAction(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId,
            @Valid @RequestBody StandardActionRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.standardAction(campaignId, battleId, combatantId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Standard action taken"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "contest" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    /**
     * Устанавливает или снимает ручной GM-override скорости комбатанта (фаза 2.11).
     *
     * @param campaignId  идентификатор кампании
     * @param battleId    идентификатор боя
     * @param combatantId идентификатор комбатанта
     * @param ft          новая скорость в футах, либо не передавать для снятия override
     * @param auth        аутентификация инициатора (нужны права GM/админа)
     * @return обёрнутое актуальное состояние боя
     */
    @PatchMapping("/{battleId}/combatants/{combatantId}/speed")
    @Operation(summary = "GM sets or clears a combatant's manual speed override (Phase 2.11)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> setSpeedOverride(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId,
            @RequestParam(required = false) Integer ft, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.setSpeedOverride(campaignId, battleId, combatantId, ft, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, ft == null ? "Speed override cleared" : "Speed override set"));
        }, controllerTaskExecutor);
    }

    /**
     * Поднимает комбатанта в воздух или приземляет (устойчивый полёт, фаза 2.13).
     *
     * @param campaignId  идентификатор кампании
     * @param battleId    идентификатор боя
     * @param combatantId идентификатор комбатанта
     * @param on          {@code true} — в полёт, {@code false} — на землю
     * @param auth        аутентификация инициатора (владелец или GM)
     * @return обёрнутое актуальное состояние боя
     */
    @PatchMapping("/{battleId}/combatants/{combatantId}/flying")
    @Operation(summary = "Set a combatant's persistent flying state (Phase 2.13)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> setFlying(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId,
            @RequestParam boolean on, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.setFlying(campaignId, battleId, combatantId, on, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, on ? "Airborne" : "Landed"));
        }, controllerTaskExecutor);
    }

    @PatchMapping("/{battleId}/combatants/{combatantId}/identity")
    @Operation(summary = "GM hides or reveals a monster's identity in the tracker (players see a generic label)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> setIdentityHidden(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId,
            @RequestParam boolean hidden, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.setIdentityHidden(campaignId, battleId, combatantId, hidden, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, hidden ? "Identity hidden" : "Identity revealed"));
        }, controllerTaskExecutor);
    }

    @PatchMapping("/{battleId}/combatants/{combatantId}/surprised")
    @Operation(summary = "GM marks a combatant surprised — can't act on round 1 (Phase 3.7)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> setSurprised(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId,
            @RequestParam boolean surprised, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.setSurprised(campaignId, battleId, combatantId, surprised, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, surprised ? "Surprised" : "No longer surprised"));
        }, controllerTaskExecutor);
    }

    /**
     * Подготовка действия (Ready, фаза 3.7): комбатант тратит действие, чтобы отложить его до триггера.
     *
     * @param campaignId  идентификатор кампании
     * @param battleId    идентификатор боя
     * @param combatantId идентификатор комбатанта
     * @param request     описание подготовленного действия и триггера
     * @param auth        аутентификация инициатора (контролёр комбатанта или GM)
     * @return обёрнутое актуальное состояние боя
     */
    @PostMapping("/{battleId}/combatants/{combatantId}/ready")
    @Operation(summary = "Ready an action with a trigger (Phase 3.7)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> readyAction(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId,
            @Valid @RequestBody ReadyActionRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.readyAction(campaignId, battleId, combatantId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Action readied"));
        }, controllerTaskExecutor);
    }

    /**
     * Срабатывание подготовленного действия (Ready, фаза 3.7) по триггеру — тратит реакцию.
     *
     * @param campaignId  идентификатор кампании
     * @param battleId    идентификатор боя
     * @param combatantId идентификатор комбатанта
     * @param auth        аутентификация инициатора (контролёр комбатанта или GM)
     * @return обёрнутое актуальное состояние боя
     */
    @PostMapping("/{battleId}/combatants/{combatantId}/ready/trigger")
    @Operation(summary = "Trigger a readied action — spends the reaction (Phase 3.7)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> triggerReady(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.triggerReady(campaignId, battleId, combatantId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Readied action triggered"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/combatants/{combatantId}/legendary-resistance")
    @Operation(summary = "GM spends a Legendary Resistance use to auto-succeed a failed save")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> useLegendaryResistance(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.useLegendaryResistance(campaignId, battleId, combatantId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Legendary Resistance used"));
        }, controllerTaskExecutor);
    }

    /**
     * Принудительно перемещает комбатанта (push/pull/slide, фаза 2.12).
     *
     * @param campaignId идентификатор кампании
     * @param battleId   идентификатор боя
     * @param request    тип/цель/клетки перемещения
     * @param auth       аутентификация инициатора (GM/админ)
     * @return обёрнутое актуальное состояние боя
     */
    @PostMapping("/{battleId}/forced-move")
    @Operation(summary = "Forced movement — push / pull / slide a combatant (GM, Phase 2.12)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> forcedMove(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @Valid @RequestBody ForcedMoveRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.forcedMovement(campaignId, battleId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Forced move applied"));
        }, controllerTaskExecutor);
    }

    /**
     * Телепортирует комбатанта, при необходимости с прихватом союзников (фаза 2.12).
     *
     * @param campaignId идентификатор кампании
     * @param battleId   идентификатор боя
     * @param request    инициатор, точка назначения, дальность и список союзников
     * @param auth       аутентификация инициатора (владелец или GM)
     * @return обёрнутое актуальное состояние боя
     */
    /**
     * Срабатывание ловушки по цели (фаза 3.2): резолв спасброска/урона и лог.
     *
     * @param campaignId идентификатор кампании
     * @param battleId   идентификатор боя
     * @param request    цель + параметры сейва/урона ловушки
     * @param auth       аутентификация инициатора (GM)
     * @return обёрнутое актуальное состояние боя
     */
    @PostMapping("/{battleId}/trap-trigger")
    @Operation(summary = "Trigger a trap on a combatant — save/damage resolution (GM, Phase 3.2)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> triggerTrap(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @Valid @RequestBody TrapTriggerRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.triggerTrap(campaignId, battleId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Trap triggered"));
        }, controllerTaskExecutor);
    }

    /**
     * Падение комбатанта с высоты (фаза 3.4): урон 1к6/10фт (кап 20к6) + prone, реюзом HP/condition-примитивов.
     *
     * @param campaignId идентификатор кампании
     * @param battleId   идентификатор боя
     * @param request    падающий комбатант, высота, готовый урон и флаг prone
     * @param auth       аутентификация инициатора (контролёр комбатанта или GM)
     * @return обёрнутое актуальное состояние боя
     */
    @PostMapping("/{battleId}/fall")
    @Operation(summary = "Apply fall damage + prone to a combatant (Phase 3.4)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> fall(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @Valid @RequestBody FallRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.fall(campaignId, battleId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Fall applied"));
        }, controllerTaskExecutor);
    }

    /**
     * Откат последней обратимой операции боя (фаза 3.5): HP / условие / позиция. Только GM.
     *
     * @param campaignId идентификатор кампании
     * @param battleId   идентификатор боя
     * @param auth       аутентификация инициатора (GM)
     * @return обёрнутое актуальное состояние боя после отката
     */
    @PostMapping("/{battleId}/undo")
    @Operation(summary = "Undo the last reversible battle operation — HP/condition/position (GM, Phase 3.5)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> undo(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.undo(campaignId, battleId, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Undone"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/teleport")
    @Operation(summary = "Teleport a combatant, optionally bringing nearby allies (Phase 2.12)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> teleport(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @Valid @RequestBody TeleportRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.teleport(campaignId, battleId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Teleported"));
        }, controllerTaskExecutor);
    }

    @PostMapping("/{battleId}/combatants/{combatantId}/contest")
    @Operation(summary = "Opposed melee contest — Grapple or Shove — against a target (own turn)")
    public CompletableFuture<ResponseEntity<ApiResponse<ContestResultResponse>>> contest(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @PathVariable UUID combatantId,
            @Valid @RequestBody ContestRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            ContestResultResponse data = battleService.contest(campaignId, battleId, combatantId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Contest resolved"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "adjust action economy" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Выполняет операции "apply combatant hp" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Выполняет операции "end battle" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Добавляет результат операции "add condition" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Удаляет результат операции "remove condition" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param conditionId идентификатор condition, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Выполняет операции "death save" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Выполняет операции "concentration check" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Выполняет операции "stabilize" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Применяет заклинание операции "cast spell" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Выполняет операции "bulk action" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{battleId}/bulk-action")
    @Operation(summary = "Mass GM operation (damage/heal/condition) over several combatants at once")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> bulkAction(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @Valid @RequestBody BulkActionRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.bulkAction(campaignId, battleId, request, auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Bulk action applied"));
        }, controllerTaskExecutor);
    }

    /**
     * Устанавливает результат операции "set initiative order" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Выполняет операции "group initiative" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @PostMapping("/{battleId}/group-initiative")
    @Operation(summary = "Roll one shared initiative die for a group of combatants (GM)")
    public CompletableFuture<ResponseEntity<ApiResponse<BattleResponse>>> groupInitiative(
            @PathVariable UUID campaignId,
            @PathVariable UUID battleId,
            @Valid @RequestBody GroupInitiativeRequest request, Authentication auth) {
        return CompletableFuture.supplyAsync(() -> {
            BattleResponse data = battleService.groupInitiative(
                    campaignId, battleId, request.getCombatantIds(), auth.getName());
            return ResponseEntity.ok(ApiResponse.ok(data, "Group initiative rolled"));
        }, controllerTaskExecutor);
    }

    /**
     * Выполняет операции "reroll initiative" в рамках бизнес-логики API.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param battleId идентификатор battle, используемый для выбора нужного бизнес-объекта
     * @param combatantId идентификатор combatant, используемый для выбора нужного бизнес-объекта
     * @param auth входящее значение auth, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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
