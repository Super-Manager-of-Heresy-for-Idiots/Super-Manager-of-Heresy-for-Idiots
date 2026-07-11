package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.BattleLogType;
import com.dnd.app.domain.enums.BattleLogVisibility;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.enums.CombatantType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.BattleAttackRequest;
import com.dnd.app.dto.response.BattleActionResultResponse;
import com.dnd.app.repository.*;
import com.dnd.app.service.combat.ClassAbilityCombatService;
import com.dnd.app.service.combat.DiceRoller;
import com.dnd.app.service.combat.WeaponAttackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
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
@DisplayName("BattleService.performAttack: монстр под управлением мастера наносит урон персонажу")
class BattleServiceAttackTest {

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
    @Mock private BattleLogService battleLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private BattleService battleService;

    @Test
    @DisplayName("Успешное попадание монстра снимает HP у персонажа и пишет в лист")
    void monsterAttack_appliesDamageToCharacter() {
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
                org.mockito.Mockito.mock(com.dnd.app.integration.map.MapZoneCreator.class), org.mockito.Mockito.mock(com.dnd.app.integration.map.MapTokenMover.class));

        String username = "gm";
        UUID campaignId = UUID.randomUUID();
        UUID battleId = UUID.randomUUID();

        User gm = User.builder().id(UUID.randomUUID()).username(username).role(Role.ADMIN).build();
        User playerOwner = User.builder().id(UUID.randomUUID()).username("player").role(Role.PLAYER).build();
        Campaign campaign = Campaign.builder().id(campaignId).build();
        Battle battle = Battle.builder()
                .id(battleId).campaign(campaign).status(BattleStatus.ACTIVE)
                .roundNumber(1).currentTurnIndex(0).build();

        // Monster attacker with a real damage feature (Cyrillic dice as in the bestiary CSV)
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
                .currentHp(20).maxHp(20).tempHp(0).armorClass(12).build();
        BattleCombatant characterC = BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle).type(CombatantType.CHARACTER).character(character)
                .displayName("Герой").turnOrder(1).currentHp(20).maxHp(20)
                .createdAt(Instant.parse("2026-01-01T00:00:01Z")).build();

        List<BattleCombatant> combatants = List.of(monsterC, characterC);

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(gm));
        when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        when(battleRepository.findByIdAndCampaignIdForUpdate(battleId, campaignId)).thenReturn(Optional.of(battle));
        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId)).thenReturn(combatants);
        when(combatantRepository.findByIdForUpdate(monsterC.getId())).thenReturn(Optional.of(monsterC));
        when(combatantRepository.findByIdForUpdate(characterC.getId())).thenReturn(Optional.of(characterC));
        when(characterRepository.findByIdForUpdate(character.getId())).thenReturn(Optional.of(character));
        when(diceRoller.rollDamage("3к4", false)).thenReturn(7);

        BattleAttackRequest req = BattleAttackRequest.builder()
                .targetCombatantId(characterC.getId()).attackName("Укус").d20(15).build();

        BattleActionResultResponse result = battleService.performAttack(campaignId, battleId, req, username);

        assertEquals("HIT", result.getOutcome(), "d20 15 + 5 = 20 против КЗ 12 — попадание");
        assertEquals(7, result.getDamage());
        assertEquals(13, result.getTargetCurrentHp(), "20 - 7 = 13");
        assertEquals(13, character.getCurrentHp(), "урон должен записаться в лист персонажа");
        assertEquals(13, characterC.getCurrentHp(), "и в строку трекера");
        verify(characterRepository).save(character);
        // Combat log (1.2): the attack records an ATTACK entry and the HP primitive a DAMAGE entry.
        verify(battleLogService).append(eq(battleId), eq(campaignId), eq(BattleLogType.ATTACK),
                eq(monsterC.getId()), eq(characterC.getId()), anyMap(), eq(BattleLogVisibility.PUBLIC), any());
        verify(battleLogService).append(eq(battleId), eq(campaignId), eq(BattleLogType.DAMAGE),
                isNull(), eq(characterC.getId()), anyMap(), eq(BattleLogVisibility.PUBLIC), any());
    }

    @Test
    @DisplayName("Урон оружейной атаки берётся из описания (нет структурных строк урона)")
    void monsterAttack_parsesDamageFromDescription() {
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
                org.mockito.Mockito.mock(com.dnd.app.integration.map.MapZoneCreator.class), org.mockito.Mockito.mock(com.dnd.app.integration.map.MapTokenMover.class));

        String username = "gm";
        UUID campaignId = UUID.randomUUID();
        UUID battleId = UUID.randomUUID();

        User gm = User.builder().id(UUID.randomUUID()).username(username).role(Role.ADMIN).build();
        User playerOwner = User.builder().id(UUID.randomUUID()).username("player").role(Role.PLAYER).build();
        Campaign campaign = Campaign.builder().id(campaignId).build();
        Battle battle = Battle.builder()
                .id(battleId).campaign(campaign).status(BattleStatus.ACTIVE)
                .roundNumber(1).currentTurnIndex(0).build();

        // Attack feature with NO structured damages — damage only in the description text.
        MonsterFeature claw = MonsterFeature.builder()
                .id(UUID.randomUUID()).section("actions").sortOrder(0).kind("action")
                .nameRusloc("Коготь").attackType("melee").attackBonus((short) 5)
                .damages(new java.util.ArrayList<>())
                .descriptionRusloc("Коготь . Рукопашная атака оружием : +5 , досягаемость 5 фт. "
                        + "Попадание : 7 ( 1к6 + 4 ) рубящего урона.").build();
        Monster monster = Monster.builder()
                .id(UUID.randomUUID()).nameRusloc("Совомедведь").armorClass((short) 13)
                .crValue(new BigDecimal("3")).xpBase(700)
                .features(new java.util.ArrayList<>(List.of(claw))).build();

        BattleCombatant monsterC = BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle).type(CombatantType.MONSTER).monster(monster)
                .displayName("Совомедведь #1").turnOrder(0).currentHp(59).maxHp(59)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z")).build();

        PlayerCharacter character = PlayerCharacter.builder()
                .id(UUID.randomUUID()).name("Герой").owner(playerOwner)
                .currentHp(38).maxHp(38).tempHp(0).armorClass(12).build();
        BattleCombatant characterC = BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle).type(CombatantType.CHARACTER).character(character)
                .displayName("Герой").turnOrder(1).currentHp(38).maxHp(38)
                .createdAt(Instant.parse("2026-01-01T00:00:01Z")).build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(gm));
        when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        when(battleRepository.findByIdAndCampaignIdForUpdate(battleId, campaignId)).thenReturn(Optional.of(battle));
        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId))
                .thenReturn(List.of(monsterC, characterC));
        when(combatantRepository.findByIdForUpdate(monsterC.getId())).thenReturn(Optional.of(monsterC));
        when(combatantRepository.findByIdForUpdate(characterC.getId())).thenReturn(Optional.of(characterC));
        when(characterRepository.findByIdForUpdate(character.getId())).thenReturn(Optional.of(character));
        // The dice parsed from the description must be the doubled-on-crit "1к6 + 4".
        when(diceRoller.rollDamage("1к6 + 4", true)).thenReturn(14);

        BattleAttackRequest req = BattleAttackRequest.builder()
                .targetCombatantId(characterC.getId()).attackName("Коготь").d20(20).build();

        BattleActionResultResponse result = battleService.performAttack(campaignId, battleId, req, username);

        assertEquals("CRIT", result.getOutcome());
        assertEquals(14, result.getDamage(), "урон должен прийти из описания, а не быть 0");
        assertEquals(24, character.getCurrentHp(), "38 - 14 = 24");
        verify(diceRoller).rollDamage("1к6 + 4", true);
    }
}
