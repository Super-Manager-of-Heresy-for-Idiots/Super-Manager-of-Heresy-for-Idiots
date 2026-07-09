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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BattleService.performAttack: режимы броска (преимущество / помеха, виртуальные и ручные кости)")
class BattleServiceRollModeTest {

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
                org.mockito.Mockito.mock(SpellCastService.class));

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
        lenient().when(diceRoller.rollDamage(eq("3к4"), anyBoolean())).thenReturn(5);
    }

    private BattleActionResultResponse attack(BattleAttackRequest req) {
        return battleService.performAttack(campaignId, battleId, req, username);
    }

    private BattleAttackRequest.BattleAttackRequestBuilder base() {
        return BattleAttackRequest.builder().targetCombatantId(characterC.getId()).attackName("Укус");
    }

    @Test
    @DisplayName("NORMAL виртуальный: один бросок d20")
    void normalVirtual_usesSingleD20() {
        when(diceRoller.rollD20()).thenReturn(15);

        BattleActionResultResponse r = attack(base().rollMode(AttackRollMode.NORMAL).build());

        assertEquals("NORMAL", r.getRollMode());
        assertEquals(15, r.getEffectiveD20());
        assertEquals(15, r.getD20());
        verify(diceRoller, times(1)).rollD20();
    }

    @Test
    @DisplayName("ADVANTAGE виртуальный: два d20, берётся больший")
    void advantageVirtual_usesMaxOfTwo() {
        when(diceRoller.rollD20()).thenReturn(8, 18);

        BattleActionResultResponse r = attack(base().rollMode(AttackRollMode.ADVANTAGE).build());

        assertEquals("ADVANTAGE", r.getRollMode());
        assertEquals(8, r.getD20A());
        assertEquals(18, r.getD20B());
        assertEquals(18, r.getEffectiveD20());
        verify(diceRoller, times(2)).rollD20();
    }

    @Test
    @DisplayName("DISADVANTAGE виртуальный: два d20, берётся меньший")
    void disadvantageVirtual_usesMinOfTwo() {
        when(diceRoller.rollD20()).thenReturn(8, 18);

        BattleActionResultResponse r = attack(base().rollMode(AttackRollMode.DISADVANTAGE).build());

        assertEquals("DISADVANTAGE", r.getRollMode());
        assertEquals(8, r.getEffectiveD20());
        verify(diceRoller, times(2)).rollD20();
    }

    @Test
    @DisplayName("NORMAL ручной: принимает одиночный d20")
    void normalManual_acceptsSingle() {
        BattleActionResultResponse r = attack(base().rollMode(AttackRollMode.NORMAL).d20(17).build());

        assertEquals(17, r.getEffectiveD20());
        verify(diceRoller, never()).rollD20();
    }

    @Test
    @DisplayName("ADVANTAGE ручной: d20A/d20B, берётся больший")
    void advantageManual_usesMax() {
        BattleActionResultResponse r = attack(base().rollMode(AttackRollMode.ADVANTAGE).d20A(12).d20B(17).build());

        assertEquals(17, r.getEffectiveD20());
        verify(diceRoller, never()).rollD20();
    }

    @Test
    @DisplayName("DISADVANTAGE ручной: d20A/d20B, берётся меньший")
    void disadvantageManual_usesMin() {
        BattleActionResultResponse r = attack(base().rollMode(AttackRollMode.DISADVANTAGE).d20A(12).d20B(17).build());

        assertEquals(12, r.getEffectiveD20());
    }

    @Test
    @DisplayName("ADVANTAGE с единственным ручным d20 отклоняется")
    void advantageSingleManual_rejected() {
        BattleAttackRequest req = base().rollMode(AttackRollMode.ADVANTAGE).d20(15).build();

        assertThrows(BadRequestException.class, () -> attack(req));
    }

    @Test
    @DisplayName("Совместимость: без rollMode и с одиночным d20 работает как раньше")
    void legacy_noModeSingleD20() {
        BattleActionResultResponse r = attack(base().d20(20).build());

        assertEquals("NORMAL", r.getRollMode());
        assertEquals(20, r.getEffectiveD20());
        assertEquals("CRIT", r.getOutcome());
        verify(diceRoller, never()).rollD20();
    }
}
