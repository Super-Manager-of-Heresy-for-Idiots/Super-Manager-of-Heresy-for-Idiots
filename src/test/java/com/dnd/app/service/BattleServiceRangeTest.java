package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.AttackRollMode;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.enums.CombatantType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.BattleAttackRequest;
import com.dnd.app.dto.response.BattleActionResultResponse;
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
import static org.mockito.Mockito.*;

/**
 * Phase 2.5 range/reach gate: a strike out of reach or a shot beyond long range is rejected;
 * long range and shooting into a melee threat force disadvantage; the GM can override the gate.
 * Positions come from the request (grid squares); distance is Chebyshev × 5 ft.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BattleService.performAttack: гейт дистанции/досягаемости (фаза 2.5)")
class BattleServiceRangeTest {

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

        // Melee bite (reach null → default 5 ft) and a ranged bow (30/60 ft).
        FeatureDamage biteDmg = FeatureDamage.builder().id(UUID.randomUUID()).sortOrder(0).dice("3к4").build();
        MonsterFeature bite = MonsterFeature.builder()
                .id(UUID.randomUUID()).section("actions").sortOrder(1).kind("action")
                .nameRusloc("Укус").attackType("melee").attackBonus((short) 5)
                .damages(new java.util.ArrayList<>(List.of(biteDmg)))
                .descriptionRusloc("bite").build();
        FeatureDamage bowDmg = FeatureDamage.builder().id(UUID.randomUUID()).sortOrder(0).dice("1к8").build();
        MonsterFeature bow = MonsterFeature.builder()
                .id(UUID.randomUUID()).section("actions").sortOrder(2).kind("action")
                .nameRusloc("Лук").attackType("ranged").attackBonus((short) 4)
                .rangeFt((short) 30).rangeLongFt((short) 60)
                .damages(new java.util.ArrayList<>(List.of(bowDmg)))
                .descriptionRusloc("bow").build();
        Monster monster = Monster.builder()
                .id(UUID.randomUUID()).nameRusloc("Гоблин").armorClass((short) 15)
                .crValue(new BigDecimal("1")).xpBase(100)
                .features(new java.util.ArrayList<>(List.of(bite, bow))).build();

        BattleCombatant monsterC = BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle).type(CombatantType.MONSTER).monster(monster)
                .displayName("Гоблин #1").turnOrder(0).currentHp(7).maxHp(7)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z")).build();

        PlayerCharacter character = PlayerCharacter.builder()
                .id(UUID.randomUUID()).name("Герой").owner(playerOwner)
                .currentHp(40).maxHp(40).tempHp(0).armorClass(12).build();
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

    private BattleActionResultResponse attack(BattleAttackRequest req) {
        return battleService.performAttack(campaignId, battleId, req, username);
    }

    private BattleAttackRequest.BattleAttackRequestBuilder melee() {
        return BattleAttackRequest.builder().targetCombatantId(characterC.getId()).attackName("Укус");
    }

    private BattleAttackRequest.BattleAttackRequestBuilder ranged() {
        return BattleAttackRequest.builder().targetCombatantId(characterC.getId()).attackName("Лук");
    }

    @Test
    @DisplayName("Ближняя атака вне досягаемости (15 фт > 5) отклоняется")
    void melee_outOfReach_rejected() {
        BattleAttackRequest req = melee().d20(15)
                .attackerCol(0).attackerRow(0).targetCol(3).targetRow(0).build();
        assertThrows(BadRequestException.class, () -> attack(req));
    }

    @Test
    @DisplayName("Ближняя атака в упор (соседняя клетка, 5 фт) проходит")
    void melee_adjacent_resolves() {
        BattleActionResultResponse r = attack(melee().d20(15)
                .attackerCol(0).attackerRow(0).targetCol(1).targetRow(1).build());
        assertEquals("IN_REACH", r.getRangeNote());
        assertEquals(5, r.getDistanceFt());
    }

    @Test
    @DisplayName("GM-обход дистанции: атака вне досягаемости всё равно проходит")
    void melee_gmOverride_resolves() {
        BattleActionResultResponse r = attack(melee().d20(15)
                .attackerCol(0).attackerRow(0).targetCol(5).targetRow(0)
                .gmOverrideRange(true).build());
        assertEquals("OUT_OF_REACH", r.getRangeNote());
        assertNotNull(r.getOutcome());
    }

    @Test
    @DisplayName("Дальняя атака в пределах нормальной дистанции — без помехи")
    void ranged_withinNormal_noDisadvantage() {
        BattleActionResultResponse r = attack(ranged().d20(15)
                .attackerCol(0).attackerRow(0).targetCol(4).targetRow(0).build()); // 20 ft ≤ 30
        assertEquals("IN_RANGE", r.getRangeNote());
        assertEquals("NORMAL", r.getRollMode());
    }

    @Test
    @DisplayName("Большая дистанция (50 фт, 30<x≤60): сервер форсирует помеху")
    void ranged_longRange_forcesDisadvantage() {
        when(diceRoller.rollD20()).thenReturn(18, 3);
        BattleActionResultResponse r = attack(ranged() // no dice → server rolls the enforced disadvantage
                .attackerCol(0).attackerRow(0).targetCol(10).targetRow(0).build()); // 50 ft
        assertEquals("LONG_RANGE", r.getRangeNote());
        assertEquals("DISADVANTAGE", r.getRollMode());
        assertEquals(3, r.getEffectiveD20());
        verify(diceRoller, times(2)).rollD20();
    }

    @Test
    @DisplayName("За пределами дальней дистанции (>60 фт) отклоняется")
    void ranged_beyondLong_rejected() {
        BattleAttackRequest req = ranged().d20(15)
                .attackerCol(0).attackerRow(0).targetCol(13).targetRow(0).build(); // 65 ft > 60
        assertThrows(BadRequestException.class, () -> attack(req));
    }

    @Test
    @DisplayName("Стрельба под угрозой ближнего боя форсирует помеху")
    void ranged_inMelee_forcesDisadvantage() {
        when(diceRoller.rollD20()).thenReturn(17, 4);
        BattleActionResultResponse r = attack(ranged()
                .attackerCol(0).attackerRow(0).targetCol(2).targetRow(0) // 10 ft, within normal
                .attackerInMeleeThreat(true).build());
        assertEquals("RANGED_IN_MELEE", r.getRangeNote());
        assertEquals("DISADVANTAGE", r.getRollMode());
        assertEquals(4, r.getEffectiveD20());
    }

    @Test
    @DisplayName("Без координат гейт пропускается (обратная совместимость)")
    void noCoords_gateSkipped() {
        BattleActionResultResponse r = attack(melee().d20(15).build());
        assertNull(r.getRangeNote());
        assertNull(r.getDistanceFt());
    }

    // ---- Cover (Phase 2.6) — target character AC 12, bite bonus +5 --------------------------------

    @Test
    @DisplayName("Без укрытия: 8+5=13 против AC 12 — попадание")
    void noCover_hitsAt12() {
        BattleActionResultResponse r = attack(melee().d20(8).build());
        assertEquals("HIT", r.getOutcome());
        assertEquals(12, r.getTargetAc());
        assertNull(r.getCover());
    }

    @Test
    @DisplayName("Половинное укрытие (+2): AC 14, тот же бросок 13 — промах")
    void halfCover_raisesAcToMiss() {
        BattleActionResultResponse r = attack(melee().d20(8)
                .cover(com.dnd.app.domain.enums.CoverType.HALF).build());
        assertEquals("MISS", r.getOutcome());
        assertEquals(14, r.getTargetAc());
        assertEquals("HALF", r.getCover());
    }

    @Test
    @DisplayName("Укрытие на ¾ (+5): AC 17")
    void threeQuartersCover_raisesAcBy5() {
        BattleActionResultResponse r = attack(melee().d20(8)
                .cover(com.dnd.app.domain.enums.CoverType.THREE_QUARTERS).build());
        assertEquals(17, r.getTargetAc());
        assertEquals("THREE_QUARTERS", r.getCover());
    }

    @Test
    @DisplayName("Полное укрытие: атака отклоняется")
    void totalCover_rejected() {
        BattleAttackRequest req = melee().d20(8)
                .cover(com.dnd.app.domain.enums.CoverType.TOTAL).build();
        assertThrows(BadRequestException.class, () -> attack(req));
    }
}
