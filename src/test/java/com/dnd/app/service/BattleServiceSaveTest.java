package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.AttackRollMode;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.enums.CombatantType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.combat.ModifierTarget;
import com.dnd.app.dto.request.BattleAttackRequest;
import com.dnd.app.dto.response.BattleActionResultResponse;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BattleService.performAttack: спасброски (бонус цели, способность, adv/disadv, граница DC)")
class BattleServiceSaveTest {

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
    private final UUID dexStatId = UUID.randomUUID();

    private Battle battle;
    private BestiaryAbility dexAbility;
    private BattleCombatant attackerC; // breath-weapon dragon (DC 13 DEX save, 6к6)
    private User playerOwner;

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
                org.mockito.Mockito.mock(FeatureEffectService.class));

        User gm = User.builder().id(UUID.randomUUID()).username(username).role(Role.ADMIN).build();
        playerOwner = User.builder().id(UUID.randomUUID()).username("player").role(Role.PLAYER).build();
        Campaign campaign = Campaign.builder().id(campaignId).build();
        battle = Battle.builder().id(battleId).campaign(campaign).status(BattleStatus.ACTIVE)
                .roundNumber(1).currentTurnIndex(0).build();

        dexAbility = BestiaryAbility.builder().id(UUID.randomUUID()).code("DEXTERITY").nameRusloc("Ловкость").build();
        FeatureDamage breathDmg = FeatureDamage.builder().id(UUID.randomUUID()).sortOrder(0).dice("6к6").build();
        MonsterFeature breath = MonsterFeature.builder()
                .id(UUID.randomUUID()).section("actions").sortOrder(1).kind("action")
                .nameRusloc("Дыхание").attackType("special").saveDc((short) 13).saveAbility(dexAbility)
                .damages(new ArrayList<>(List.of(breathDmg))).descriptionRusloc("breath").build();
        Monster dragon = Monster.builder()
                .id(UUID.randomUUID()).nameRusloc("Дракон").armorClass((short) 18)
                .crValue(new BigDecimal("5")).xpBase(1000)
                .features(new ArrayList<>(List.of(breath))).build();
        attackerC = BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle).type(CombatantType.MONSTER).monster(dragon)
                .displayName("Дракон").turnOrder(0).currentHp(100).maxHp(100)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z")).build();

        lenient().when(userRepository.findByUsername(username)).thenReturn(Optional.of(gm));
        lenient().when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        lenient().when(battleRepository.findByIdAndCampaignIdForUpdate(battleId, campaignId)).thenReturn(Optional.of(battle));
        lenient().when(combatantRepository.findByIdForUpdate(attackerC.getId())).thenReturn(Optional.of(attackerC));
        lenient().when(combatantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(diceRoller.rollDamage(eq("6к6"), any(Boolean.class))).thenReturn(12);
    }

    private BattleActionResultResponse breathAt(BattleCombatant target, BattleAttackRequest.BattleAttackRequestBuilder req) {
        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId)).thenReturn(List.of(attackerC, target));
        when(combatantRepository.findByIdForUpdate(target.getId())).thenReturn(Optional.of(target));
        return battleService.performAttack(campaignId, battleId,
                req.targetCombatantId(target.getId()).attackName("Дыхание").build(), username);
    }

    private BattleCombatant monsterTarget(Short dexScore, MonsterSavingThrow... saves) {
        Monster m = Monster.builder().id(UUID.randomUUID()).nameRusloc("Цель").dexScore(dexScore)
                .savingThrows(new ArrayList<>(List.of(saves))).build();
        return BattleCombatant.builder().id(UUID.randomUUID()).battle(battle).type(CombatantType.MONSTER).monster(m)
                .displayName("Цель").turnOrder(1).currentHp(30).maxHp(30)
                .createdAt(Instant.parse("2026-01-01T00:00:01Z")).build();
    }

    private BattleCombatant characterTarget(int dexScore, boolean proficient) {
        StatType dexStat = StatType.builder().id(dexStatId).slug("dex").build();
        CharacterStat cs = CharacterStat.builder().id(UUID.randomUUID()).statType(dexStat).value(dexScore).build();
        PlayerCharacter ch = PlayerCharacter.builder()
                .id(UUID.randomUUID()).name("Плут").owner(playerOwner).totalLevel(5)
                .currentHp(30).maxHp(30).tempHp(0).armorClass(14)
                .stats(new ArrayList<>(List.of(cs)))
                .savingThrowProficiencyStatIdsJson(proficient ? "[\"" + dexStatId + "\"]" : null)
                .build();
        lenient().when(characterRepository.findByIdForUpdate(ch.getId())).thenReturn(Optional.of(ch));
        lenient().when(modifierAggregator.totalFor(eq(ch.getId()), any(ModifierTarget.class))).thenReturn(0);
        return BattleCombatant.builder().id(UUID.randomUUID()).battle(battle).type(CombatantType.CHARACTER).character(ch)
                .displayName("Плут").turnOrder(1).currentHp(30).maxHp(30)
                .createdAt(Instant.parse("2026-01-01T00:00:01Z")).build();
    }

    @Test
    @DisplayName("Монстр со спасброском в статблоке: используется его бонус; total==DC → успех")
    void monsterStatblockSave_boundarySuccess() {
        BattleCombatant target = monsterTarget((short) 10,
                MonsterSavingThrow.builder().ability(dexAbility).bonus((short) 5).build());

        BattleActionResultResponse r = breathAt(target, BattleAttackRequest.builder().saveD20(8));

        assertEquals("SUCCESS", r.getOutcome());
        assertEquals("Ловкость", r.getSaveAbility());
        assertEquals(5, r.getSaveBonus());
        assertEquals(13, r.getSaveTotal()); // 8 + 5 == DC 13
        assertEquals(6, r.getDamage());     // 12 halved
    }

    @Test
    @DisplayName("Монстр без спасброска в статблоке: берётся модификатор способности (DEX 14 → +2), провал")
    void monsterAbilityModFallback_fail() {
        BattleCombatant target = monsterTarget((short) 14); // no statblock saves

        BattleActionResultResponse r = breathAt(target, BattleAttackRequest.builder().saveD20(8));

        assertEquals("FAIL", r.getOutcome());
        assertEquals(2, r.getSaveBonus());  // +2 from DEX 14
        assertEquals(10, r.getSaveTotal()); // 8 + 2
        assertEquals(12, r.getDamage());    // full
    }

    @Test
    @DisplayName("Персонаж с владением сейвом: мод + бонус мастерства (DEX 14 +2, ур.5 +3 = +5), успех")
    void characterProficient_success() {
        BattleCombatant target = characterTarget(14, true);

        BattleActionResultResponse r = breathAt(target, BattleAttackRequest.builder().saveD20(8));

        assertEquals("SUCCESS", r.getOutcome());
        assertEquals(5, r.getSaveBonus());  // +2 + prof 3
        assertEquals(13, r.getSaveTotal());
        assertEquals(6, r.getDamage());
    }

    @Test
    @DisplayName("Персонаж без владения: только модификатор способности (+2), провал")
    void characterNotProficient_fail() {
        BattleCombatant target = characterTarget(14, false);

        BattleActionResultResponse r = breathAt(target, BattleAttackRequest.builder().saveD20(8));

        assertEquals("FAIL", r.getOutcome());
        assertEquals(2, r.getSaveBonus());
        assertEquals(10, r.getSaveTotal());
        assertEquals(12, r.getDamage());
    }

    @Test
    @DisplayName("Преимущество на спасброске: берётся больший из двух кубов")
    void advantageSave_keepsHigher() {
        BattleCombatant target = monsterTarget((short) 10,
                MonsterSavingThrow.builder().ability(dexAbility).bonus((short) 5).build());

        BattleActionResultResponse r = breathAt(target, BattleAttackRequest.builder()
                .saveRollMode(AttackRollMode.ADVANTAGE).saveD20A(3).saveD20B(15));

        assertEquals("ADVANTAGE", r.getSaveRollMode());
        assertEquals(15, r.getEffectiveD20());
        assertEquals("SUCCESS", r.getOutcome());
        assertEquals(20, r.getSaveTotal()); // 15 + 5
    }
}
