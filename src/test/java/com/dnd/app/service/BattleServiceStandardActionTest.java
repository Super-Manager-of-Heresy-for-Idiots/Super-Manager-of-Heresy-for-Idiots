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

    @BeforeEach
    void setUp() {
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
                org.mockito.Mockito.mock(BattleLogService.class),
                org.mockito.Mockito.mock(SpellCastService.class),
                org.mockito.Mockito.mock(StatTypeRepository.class),
                org.mockito.Mockito.mock(FeatureEffectService.class),
                org.mockito.Mockito.mock(com.dnd.app.integration.map.MapZoneCreator.class));

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
        Monster monster = Monster.builder()
                .id(UUID.randomUUID()).nameRusloc("Гоблин").armorClass((short) 15)
                .crValue(new BigDecimal("1")).xpBase(100)
                .features(new java.util.ArrayList<>(List.of(bite))).build();

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
}
