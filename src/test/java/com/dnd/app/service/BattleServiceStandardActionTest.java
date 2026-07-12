package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.AttackRollMode;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.enums.CombatantType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.StandardActionType;
import com.dnd.app.dto.request.BattleAttackRequest;
import com.dnd.app.dto.request.StandardActionRequest;
import com.dnd.app.dto.response.BattleActionResultResponse;
import com.dnd.app.dto.response.BattleCombatantResponse;
import com.dnd.app.dto.response.BattleResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.*;
import com.dnd.app.service.combat.ClassAbilityCombatService;
import com.dnd.app.service.combat.DiceRoller;
import com.dnd.app.service.combat.WeaponAttackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 2.7 standard actions: Dash / Dodge / Disengage / Help / Hide spend the action economy and set
 * a turn-scoped state, and that state shapes the attack roll (dodge → disadvantage on attackers;
 * hiding / an ally's Help → advantage, consumed on the attack; advantage and disadvantage cancel).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BattleService: стандартные действия (Dash/Dodge/Disengage/Help/Hide) — фаза 2.7")
class BattleServiceStandardActionTest {

    @Mock private BattleRepository battleRepository;
    @Mock private BattleCombatantRepository combatantRepository;
    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private UserRepository userRepository;
    @Mock private CampaignService campaignService;
    @Mock private MonsterService monsterService;
    @Mock private CharacterService characterService;
    @Mock private CharacterResourceService characterResourceService;
    @Mock private CharacterEffectService characterEffectService;
    @Mock private WebSocketEventService webSocketEventService;
    @Mock private DiceRoller diceRoller;
    @Mock private WeaponAttackService weaponAttackService;
    @Mock private ClassAbilityCombatService classAbilityCombatService;
    @Mock private ItemInstanceRepository itemInstanceRepository;
    @Mock private SpellRepository spellRepository;
    @Mock private SpellSlotService spellSlotService;
    @Mock private GameplayEventService gameplayEventService;
    @Mock private ModifierAggregator modifierAggregator;
    @Mock private EffectExpirationService effectExpirationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private BattleService battleService;

    private final String username = "gm";
    private final UUID campaignId = UUID.randomUUID();
    private final UUID battleId = UUID.randomUUID();
    private BattleCombatant monsterC;
    private BattleCombatant characterC;
    private MonsterFeature breathFeature;
    private BattleLogService battleLogService; // мок, застабленный для undo-тестов (фаза 3.5)

    @BeforeEach
    void setUp() {
        battleLogService = org.mockito.Mockito.mock(BattleLogService.class);
        battleService = new BattleService(battleRepository, combatantRepository, characterRepository,
                userRepository, campaignService, monsterService, characterService,
                characterResourceService, characterEffectService, webSocketEventService,
                diceRoller, weaponAttackService, classAbilityCombatService, itemInstanceRepository,
                spellRepository, spellSlotService, objectMapper,
                new CharacterHpService(characterRepository, combatantRepository,
                        webSocketEventService, gameplayEventService),
                modifierAggregator, effectExpirationService,
                new DamageMitigationService(modifierAggregator),
                org.mockito.Mockito.mock(ConditionService.class),
                battleLogService,
                org.mockito.Mockito.mock(SpellCastService.class),
                org.mockito.Mockito.mock(StatTypeRepository.class),
                org.mockito.Mockito.mock(FeatureEffectService.class),
                org.mockito.Mockito.mock(com.dnd.app.integration.map.MapZoneCreator.class), org.mockito.Mockito.mock(com.dnd.app.integration.map.MapTokenMover.class), new com.dnd.app.service.CommandDedupService());

        User gm = User.builder().id(UUID.randomUUID()).username(username).role(Role.ADMIN).build();
        User playerOwner = User.builder().id(UUID.randomUUID()).username("player").role(Role.PLAYER).build();
        Campaign campaign = Campaign.builder().id(campaignId).build();
        Battle battle = Battle.builder()
                .id(battleId).campaign(campaign).status(BattleStatus.ACTIVE)
                .roundNumber(1).currentTurnIndex(0).build();

        FeatureDamage dmg = FeatureDamage.builder().id(UUID.randomUUID()).sortOrder(0).dice("3к4").build();
        MonsterFeature bite = MonsterFeature.builder()
                .id(UUID.randomUUID()).section("actions").sortOrder(1).kind("action")
                .nameRusloc("Укус").attackType("melee").attackBonus((short) 5)
                .damages(new java.util.ArrayList<>(List.of(dmg)))
                .descriptionRusloc("bite").build();
        FeatureDamage breathDmg = FeatureDamage.builder().id(UUID.randomUUID()).sortOrder(0).dice("6к6").build();
        breathFeature = MonsterFeature.builder()
                .id(UUID.randomUUID()).section("actions").sortOrder(2).kind("action")
                .nameRusloc("Дыхание").attackType("ranged").attackBonus((short) 0)
                .rechargeMin((short) 5).rechargeMax((short) 6)
                .damages(new java.util.ArrayList<>(List.of(breathDmg)))
                .descriptionRusloc("breath").build();
        Monster monster = Monster.builder()
                .id(UUID.randomUUID()).nameRusloc("Гоблин").armorClass((short) 15)
                .crValue(new BigDecimal("1")).xpBase(100)
                .features(new java.util.ArrayList<>(List.of(bite, breathFeature))).build();

        monsterC = BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle).type(CombatantType.MONSTER).monster(monster)
                .displayName("Гоблин #1").turnOrder(0).currentHp(7).maxHp(7)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z")).build();

        PlayerCharacter character = PlayerCharacter.builder()
                .id(UUID.randomUUID()).name("Герой").owner(playerOwner)
                .currentHp(40).maxHp(40).tempHp(0).armorClass(12).speed(30).build();
        characterC = BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle).type(CombatantType.CHARACTER).character(character)
                .displayName("Герой").turnOrder(1).currentHp(40).maxHp(40)
                .createdAt(Instant.parse("2026-01-01T00:00:01Z")).build();

        lenient().when(userRepository.findByUsername(username)).thenReturn(Optional.of(gm));
        lenient().when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        lenient().when(battleRepository.findByIdAndCampaignIdForUpdate(battleId, campaignId)).thenReturn(Optional.of(battle));
        lenient().when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId))
                .thenReturn(List.of(monsterC, characterC));
        lenient().when(combatantRepository.findByIdForUpdate(monsterC.getId())).thenReturn(Optional.of(monsterC));
        lenient().when(combatantRepository.findByIdForUpdate(characterC.getId())).thenReturn(Optional.of(characterC));
        lenient().when(characterRepository.findByIdForUpdate(character.getId())).thenReturn(Optional.of(character));
        lenient().when(diceRoller.rollDamage(anyString(), anyBoolean())).thenReturn(5);
    }

    private BattleCombatantResponse monsterIn(BattleResponse r) {
        return r.getCombatants().stream().filter(c -> c.getId().equals(monsterC.getId())).findFirst().orElseThrow();
    }

    private BattleCombatantResponse charIn(BattleResponse r) {
        return r.getCombatants().stream().filter(c -> c.getId().equals(characterC.getId())).findFirst().orElseThrow();
    }

    private BattleResponse standard(StandardActionRequest req) {
        return battleService.standardAction(campaignId, battleId, monsterC.getId(), req, username);
    }

    private BattleActionResultResponse monsterAttack(BattleAttackRequest req) {
        return battleService.performAttack(campaignId, battleId, req, username);
    }

    private BattleAttackRequest.BattleAttackRequestBuilder bite() {
        return BattleAttackRequest.builder().targetCombatantId(characterC.getId()).attackName("Укус");
    }

    // ---- Action mechanics ------------------------------------------------------------------------

    @Test
    @DisplayName("Dash: ставит флаг и тратит действие")
    void dash_setsFlagAndSpendsAction() {
        BattleResponse r = standard(StandardActionRequest.builder().type(StandardActionType.DASH).build());
        assertTrue(monsterIn(r).isDashing());
        assertEquals(1, monsterIn(r).getActionSpent());
    }

    @Test
    @DisplayName("Dodge/Disengage: ставят соответствующие флаги")
    void dodgeDisengage_setFlags() {
        assertTrue(monsterIn(standard(StandardActionRequest.builder().type(StandardActionType.DODGE).build())).isDodging());
        monsterC.setActionSpent(0); // new turn
        assertTrue(monsterIn(standard(StandardActionRequest.builder().type(StandardActionType.DISENGAGE).build())).isDisengaged());
    }

    @Test
    @DisplayName("Второе действие в тот же ход отклоняется")
    void secondAction_rejected() {
        standard(StandardActionRequest.builder().type(StandardActionType.DODGE).build());
        StandardActionRequest again = StandardActionRequest.builder().type(StandardActionType.DASH).build();
        assertThrows(BadRequestException.class, () -> standard(again));
    }

    @Test
    @DisplayName("Help: помечает союзника преимуществом, действие тратит помогающий")
    void help_flagsAlly() {
        BattleResponse r = standard(StandardActionRequest.builder()
                .type(StandardActionType.HELP).targetCombatantId(characterC.getId()).build());
        assertTrue(charIn(r).isHelpAdvantage());
        assertEquals(1, monsterIn(r).getActionSpent());
    }

    @Test
    @DisplayName("Help без цели отклоняется")
    void help_noTarget_rejected() {
        StandardActionRequest req = StandardActionRequest.builder().type(StandardActionType.HELP).build();
        assertThrows(BadRequestException.class, () -> standard(req));
    }

    @Test
    @DisplayName("Hide: успех при total ≥ DC (ручной d20)")
    void hide_successMeetsDc() {
        BattleResponse r = standard(StandardActionRequest.builder()
                .type(StandardActionType.HIDE).stealthD20(15).stealthBonus(3).hideDc(17).build());
        assertTrue(monsterIn(r).isHidden()); // 15 + 3 = 18 ≥ 17
    }

    @Test
    @DisplayName("Hide: провал при total < DC")
    void hide_failBelowDc() {
        BattleResponse r = standard(StandardActionRequest.builder()
                .type(StandardActionType.HIDE).stealthD20(5).stealthBonus(2).hideDc(17).build());
        assertFalse(monsterIn(r).isHidden()); // 5 + 2 = 7 < 17
    }

    // ---- Attack integration ----------------------------------------------------------------------

    @Test
    @DisplayName("Атака по уклоняющейся цели — сервер форсирует помеху")
    void attack_vsDodgingTarget_disadvantage() {
        characterC.setDodging(true);
        when(diceRoller.rollD20()).thenReturn(18, 4);
        BattleActionResultResponse r = monsterAttack(bite().build()); // no dice → server rolls
        assertEquals("DISADVANTAGE", r.getRollMode());
        assertEquals(4, r.getEffectiveD20());
        verify(diceRoller, times(2)).rollD20();
    }

    @Test
    @DisplayName("Атака из скрытности — преимущество, скрытность снимается")
    void attack_fromHiding_advantageThenRevealed() {
        monsterC.setHidden(true);
        when(diceRoller.rollD20()).thenReturn(6, 19);
        BattleActionResultResponse r = monsterAttack(bite().build());
        assertEquals("ADVANTAGE", r.getRollMode());
        assertEquals(19, r.getEffectiveD20());
        assertFalse(monsterIn(r.getBattle()).isHidden()); // consumed
    }

    @Test
    @DisplayName("Help-преимущество применяется к атаке и расходуется")
    void attack_withHelp_advantageConsumed() {
        monsterC.setHelpAdvantage(true);
        when(diceRoller.rollD20()).thenReturn(6, 19);
        BattleActionResultResponse r = monsterAttack(bite().build());
        assertEquals("ADVANTAGE", r.getRollMode());
        assertFalse(monsterIn(r.getBattle()).isHelpAdvantage()); // consumed
    }

    @Test
    @DisplayName("Преимущество (скрытность) и помеха (уклонение цели) взаимно гасятся → обычный бросок")
    void attack_advantageAndDisadvantageCancel() {
        monsterC.setHidden(true);
        characterC.setDodging(true);
        BattleActionResultResponse r = monsterAttack(bite().d20(12).build()); // single die accepted → NORMAL
        assertEquals("NORMAL", r.getRollMode());
        assertEquals(12, r.getEffectiveD20());
    }

    // ---- Grapple / Shove contests ----------------------------------------------------------------

    private com.dnd.app.dto.response.ContestResultResponse contest(com.dnd.app.dto.request.ContestRequest req) {
        return battleService.contest(campaignId, battleId, monsterC.getId(), req, username);
    }

    @Test
    @DisplayName("Grapple: атакующий выигрывает контест → цель схвачена, действие потрачено")
    void grapple_attackerWins() {
        var r = contest(com.dnd.app.dto.request.ContestRequest.builder()
                .type(com.dnd.app.domain.enums.ContestType.GRAPPLE)
                .targetCombatantId(characterC.getId())
                .attackerD20(18).attackerBonus(3).targetD20(6).targetBonus(2).build());
        assertTrue(r.isAttackerWins()); // 21 > 8
        assertEquals("grappled", r.getCondition());
        assertEquals(1, monsterIn(r.getBattle()).getActionSpent());
    }

    @Test
    @DisplayName("Contest: защитник выигрывает ничью (равные суммы) → нет условия")
    void contest_defenderWinsTies() {
        var r = contest(com.dnd.app.dto.request.ContestRequest.builder()
                .type(com.dnd.app.domain.enums.ContestType.GRAPPLE)
                .targetCombatantId(characterC.getId())
                .attackerD20(10).attackerBonus(2).targetD20(12).targetBonus(0).build());
        assertFalse(r.isAttackerWins()); // 12 == 12 → defender wins
        assertNull(r.getCondition());
    }

    @Test
    @DisplayName("Shove (по умолчанию) при победе → цель сбита с ног (prone)")
    void shove_prone() {
        var r = contest(com.dnd.app.dto.request.ContestRequest.builder()
                .type(com.dnd.app.domain.enums.ContestType.SHOVE)
                .targetCombatantId(characterC.getId())
                .attackerD20(19).attackerBonus(4).targetD20(5).targetBonus(1).build());
        assertTrue(r.isAttackerWins());
        assertEquals("prone", r.getCondition());
    }

    @Test
    @DisplayName("Shove PUSH при победе → без условия (толчок — движение, фаза 2.12)")
    void shove_push_noCondition() {
        var r = contest(com.dnd.app.dto.request.ContestRequest.builder()
                .type(com.dnd.app.domain.enums.ContestType.SHOVE).shoveMode("PUSH")
                .targetCombatantId(characterC.getId())
                .attackerD20(19).attackerBonus(4).targetD20(5).targetBonus(1).build());
        assertTrue(r.isAttackerWins());
        assertNull(r.getCondition());
    }

    // ---- Opportunity / reaction attack (Phase 2.8) -----------------------------------------------

    @Test
    @DisplayName("Реакция-атака тратит реакцию, а не действие")
    void reactionAttack_spendsReactionNotAction() {
        BattleActionResultResponse r = monsterAttack(bite().d20(15)
                .reaction(true).attackerCombatantId(monsterC.getId()).build());
        assertEquals("HIT", r.getOutcome());
        assertTrue(monsterIn(r.getBattle()).isReactionUsed());
        assertEquals(0, monsterIn(r.getBattle()).getActionSpent());
    }

    @Test
    @DisplayName("Реакция без attackerCombatantId отклоняется")
    void reactionAttack_missingAttacker_rejected() {
        BattleAttackRequest req = bite().d20(15).reaction(true).build();
        assertThrows(BadRequestException.class, () -> monsterAttack(req));
    }

    @Test
    @DisplayName("Вторая реакция в тот же ход отклоняется")
    void reactionAttack_secondReaction_rejected() {
        monsterC.setReactionUsed(true);
        BattleAttackRequest req = bite().d20(15).reaction(true).attackerCombatantId(monsterC.getId()).build();
        assertThrows(BadRequestException.class, () -> monsterAttack(req));
    }

    // ---- Legendary Resistance (Phase 2.9) --------------------------------------------------------

    @Test
    @DisplayName("Legendary Resistance: расходует использование, пока есть запас")
    void legendaryResistance_decrementsPool() {
        monsterC.setLegendaryResistanceMax(2);
        BattleResponse r1 = battleService.useLegendaryResistance(campaignId, battleId, monsterC.getId(), username);
        assertEquals(1, monsterIn(r1).getLegendaryResistanceUsed());
        BattleResponse r2 = battleService.useLegendaryResistance(campaignId, battleId, monsterC.getId(), username);
        assertEquals(2, monsterIn(r2).getLegendaryResistanceUsed());
    }

    @Test
    @DisplayName("Legendary Resistance: без запаса отклоняется")
    void legendaryResistance_exhausted_rejected() {
        monsterC.setLegendaryResistanceMax(1);
        monsterC.setLegendaryResistanceUsed(1);
        assertThrows(BadRequestException.class,
                () -> battleService.useLegendaryResistance(campaignId, battleId, monsterC.getId(), username));
    }

    // ---- Multiattack (Phase 2.9) -----------------------------------------------------------------

    @Test
    @DisplayName("Multiattack: несколько атак за одно действие, потом отказ")
    void multiattack_allowsBudgetThenRejects() {
        monsterC.setAttacksRemaining(2);
        BattleActionResultResponse r1 = monsterAttack(bite().d20(15).build());
        assertEquals(1, monsterIn(r1.getBattle()).getAttacksRemaining());
        assertEquals(0, monsterIn(r1.getBattle()).getActionSpent()); // action not spent per-attack
        BattleActionResultResponse r2 = monsterAttack(bite().d20(15).build());
        assertEquals(0, monsterIn(r2.getBattle()).getAttacksRemaining());
        assertThrows(BadRequestException.class, () -> monsterAttack(bite().d20(15).build()));
    }

    // ---- Recharge abilities (Phase 2.9) ----------------------------------------------------------

    private BattleAttackRequest.BattleAttackRequestBuilder breath() {
        return BattleAttackRequest.builder().targetCombatantId(characterC.getId()).attackName("Дыхание");
    }

    @Test
    @DisplayName("Recharge: повторное использование до перезарядки отклоняется")
    void recharge_blocksReuseUntilRecharged() {
        monsterAttack(breath().d20(15).build()); // first use spends it
        assertThrows(BadRequestException.class, () -> monsterAttack(breath().d20(15).build()));
    }

    // ---- Hidden identity (Phase 2.10) ------------------------------------------------------------

    @Test
    @DisplayName("Hidden identity: скрытие даёт публичный ярлык, раскрытие убирает его")
    void identityHidden_togglesAndExposesPublicName() {
        BattleResponse hidden = battleService.setIdentityHidden(campaignId, battleId, monsterC.getId(), true, username);
        assertTrue(monsterIn(hidden).isIdentityHidden());
        assertNotNull(monsterIn(hidden).getPublicName());
        BattleResponse shown = battleService.setIdentityHidden(campaignId, battleId, monsterC.getId(), false, username);
        assertFalse(monsterIn(shown).isIdentityHidden());
        assertNull(monsterIn(shown).getPublicName());
    }

    // ---- Traps (Phase 3.2) -----------------------------------------------------------------------

    @Test
    @DisplayName("Ловушка без сейва: полный урон применяется к цели")
    void trap_noSave_appliesDamage() {
        var req = com.dnd.app.dto.request.TrapTriggerRequest.builder()
                .targetCombatantId(monsterC.getId()).amount(5).build();
        BattleResponse r = battleService.triggerTrap(campaignId, battleId, req, username);
        assertEquals(2, monsterIn(r).getCurrentHp()); // 7 - 5
    }

    @Test
    @DisplayName("Ловушка с нулевым уроном: HP не меняется")
    void trap_zeroDamage_noChange() {
        var req = com.dnd.app.dto.request.TrapTriggerRequest.builder()
                .targetCombatantId(monsterC.getId()).amount(0).build();
        BattleResponse r = battleService.triggerTrap(campaignId, battleId, req, username);
        assertEquals(7, monsterIn(r).getCurrentHp());
    }

    // ---- Falling (Phase 3.4) ---------------------------------------------------------------------

    @Test
    @DisplayName("Падение с высоты: готовый урон применяется к цели")
    void fall_appliesDamage() {
        var req = com.dnd.app.dto.request.FallRequest.builder()
                .combatantId(monsterC.getId()).heightFt(30).manualTotal(5).build();
        BattleResponse r = battleService.fall(campaignId, battleId, req, username);
        assertEquals(2, monsterIn(r).getCurrentHp()); // 7 - 5
    }

    @Test
    @DisplayName("Падение <10 футов: урона нет, HP не меняется")
    void fall_lowHeight_noDamage() {
        var req = com.dnd.app.dto.request.FallRequest.builder()
                .combatantId(monsterC.getId()).heightFt(5).build();
        BattleResponse r = battleService.fall(campaignId, battleId, req, username);
        assertEquals(7, monsterIn(r).getCurrentHp());
    }

    @Test
    @DisplayName("Падение сбрасывает флаг полёта")
    void fall_clearsFlying() {
        battleService.setFlying(campaignId, battleId, monsterC.getId(), true, username);
        var req = com.dnd.app.dto.request.FallRequest.builder()
                .combatantId(monsterC.getId()).heightFt(20).manualTotal(3).build();
        BattleResponse r = battleService.fall(campaignId, battleId, req, username);
        assertFalse(monsterIn(r).isFlying());
    }

    // ---- Undo (Phase 3.5) ------------------------------------------------------------------------

    @Test
    @DisplayName("Undo HP: обратная дельта восстанавливает HP и помечает запись откатанной")
    void undo_hp_restoresAndMarks() {
        monsterC.setCurrentHp(2); // как будто получил 5 урона (7 → 2)
        com.dnd.app.domain.BattleLog entry = com.dnd.app.domain.BattleLog.builder()
                .battleId(battleId).seq(5).type(com.dnd.app.domain.enums.BattleLogType.DAMAGE)
                .undoPayload("{\"kind\":\"HP\",\"combatantId\":\"" + monsterC.getId() + "\",\"delta\":-5}")
                .undone(false).build();
        when(battleLogService.findLastUndoable(battleId)).thenReturn(java.util.Optional.of(entry));

        BattleResponse r = battleService.undo(campaignId, battleId, username);
        assertEquals(7, monsterIn(r).getCurrentHp()); // 2 + 5 (обратно к полному)
        verify(battleLogService).markUndone(entry);
    }

    @Test
    @DisplayName("Undo: откатывать нечего — ошибка")
    void undo_nothing_rejected() {
        when(battleLogService.findLastUndoable(battleId)).thenReturn(java.util.Optional.empty());
        assertThrows(BadRequestException.class, () -> battleService.undo(campaignId, battleId, username));
    }

    // ---- Realtime reliability (Phase 2.14) -------------------------------------------------------

    @Test
    @DisplayName("Двойной next-turn: устаревший индекс хода отклоняется")
    void endTurn_staleTurnIndex_rejected() {
        battleService.endTurn(campaignId, battleId, 0, 1, null, username); // 0 → 1
        assertThrows(BadRequestException.class,
                () -> battleService.endTurn(campaignId, battleId, 0, 1, null, username)); // индекс 0 уже неактуален
    }

    @Test
    @DisplayName("Дедуп: повтор той же команды end-turn не сдвигает ход второй раз")
    void endTurn_duplicateCommand_isNoOp() {
        UUID cmd = UUID.randomUUID();
        BattleResponse r1 = battleService.endTurn(campaignId, battleId, null, null, cmd, username);
        assertEquals(1, r1.getCurrentTurnIndex());
        BattleResponse r2 = battleService.endTurn(campaignId, battleId, null, null, cmd, username);
        assertEquals(1, r2.getCurrentTurnIndex()); // тот же id → no-op, ход не сдвинулся
    }

    // ---- Flight (Phase 2.13) ---------------------------------------------------------------------

    @Test
    @DisplayName("Полёт: поднимается и приземляется (устойчивое состояние)")
    void flying_toggles() {
        BattleResponse up = battleService.setFlying(campaignId, battleId, monsterC.getId(), true, username);
        assertTrue(monsterIn(up).isFlying());
        BattleResponse down = battleService.setFlying(campaignId, battleId, monsterC.getId(), false, username);
        assertFalse(monsterIn(down).isFlying());
    }

    // ---- Forced movement / teleport (Phase 2.12) -------------------------------------------------

    @Test
    @DisplayName("Forced move (push): в пределах максимума — применяется")
    void forcedMove_withinMax_applies() {
        var req = com.dnd.app.dto.request.ForcedMoveRequest.builder()
                .type(com.dnd.app.domain.enums.ForcedMoveType.PUSH)
                .targetCombatantId(characterC.getId())
                .fromCol(0).fromRow(0).toCol(2).toRow(0).maxDistanceFt(15).build();
        assertNotNull(battleService.forcedMovement(campaignId, battleId, req, username)); // 10 ≤ 15
    }

    @Test
    @DisplayName("Forced move: за пределами максимума — отклоняется")
    void forcedMove_beyondMax_rejected() {
        var req = com.dnd.app.dto.request.ForcedMoveRequest.builder()
                .type(com.dnd.app.domain.enums.ForcedMoveType.PUSH)
                .targetCombatantId(characterC.getId())
                .fromCol(0).fromRow(0).toCol(5).toRow(0).maxDistanceFt(15).build(); // 25 > 15
        assertThrows(BadRequestException.class,
                () -> battleService.forcedMovement(campaignId, battleId, req, username));
    }

    @Test
    @DisplayName("Teleport с прихватом союзника рядом — применяется")
    void teleport_withNearbyAlly_applies() {
        var req = com.dnd.app.dto.request.TeleportRequest.builder()
                .combatantId(characterC.getId())
                .fromCol(0).fromRow(0).toCol(3).toRow(0).rangeFt(30).allyPickupFt(10)
                .allies(List.of(com.dnd.app.dto.request.TeleportRequest.Ally.builder()
                        .combatantId(monsterC.getId()).fromCol(1).fromRow(0).toCol(4).toRow(0).build()))
                .build();
        assertNotNull(battleService.teleport(campaignId, battleId, req, username));
    }

    @Test
    @DisplayName("Teleport: союзник слишком далеко для прихвата — отклоняется")
    void teleport_allyTooFar_rejected() {
        var req = com.dnd.app.dto.request.TeleportRequest.builder()
                .combatantId(characterC.getId())
                .fromCol(0).fromRow(0).toCol(3).toRow(0).rangeFt(30).allyPickupFt(10)
                .allies(List.of(com.dnd.app.dto.request.TeleportRequest.Ally.builder()
                        .combatantId(monsterC.getId()).fromCol(5).fromRow(0).toCol(4).toRow(0).build())) // 25 > 10
                .build();
        assertThrows(BadRequestException.class,
                () -> battleService.teleport(campaignId, battleId, req, username));
    }

    @Test
    @DisplayName("Teleport: точка назначения за пределами дальности — отклоняется")
    void teleport_beyondRange_rejected() {
        var req = com.dnd.app.dto.request.TeleportRequest.builder()
                .combatantId(characterC.getId())
                .fromCol(0).fromRow(0).toCol(10).toRow(0).rangeFt(30).build(); // 50 > 30
        assertThrows(BadRequestException.class,
                () -> battleService.teleport(campaignId, battleId, req, username));
    }

    // ---- GM speed override (Phase 2.11) ----------------------------------------------------------

    @Test
    @DisplayName("GM speed override: задаётся и снимается")
    void speedOverride_setAndClear() {
        BattleResponse set = battleService.setSpeedOverride(campaignId, battleId, monsterC.getId(), 60, username);
        assertEquals(60, monsterIn(set).getSpeedOverrideFt());
        BattleResponse cleared = battleService.setSpeedOverride(campaignId, battleId, monsterC.getId(), null, username);
        assertNull(monsterIn(cleared).getSpeedOverrideFt());
    }

    @Test
    @DisplayName("GM speed override: отрицательное значение отклоняется")
    void speedOverride_negative_rejected() {
        assertThrows(BadRequestException.class,
                () -> battleService.setSpeedOverride(campaignId, battleId, monsterC.getId(), -5, username));
    }

    @Test
    @DisplayName("Recharge: бросок d6 ≥ порога в начале хода перезаряжает способность")
    void recharge_rollAtTurnStartRecharges() {
        monsterAttack(breath().d20(15).build()); // expend the breath
        when(diceRoller.rollDie(6)).thenReturn(6); // recharges (≥5)
        // End the character's turn so the monster's next turn starts and rolls recharge.
        battleService.endTurn(campaignId, battleId, username); // monster(0)→character(1)
        battleService.endTurn(campaignId, battleId, username); // character(1)→monster(0): recharge rolls
        BattleActionResultResponse again = monsterAttack(breath().d20(15).build());
        assertEquals("HIT", again.getOutcome()); // usable again
    }
}
