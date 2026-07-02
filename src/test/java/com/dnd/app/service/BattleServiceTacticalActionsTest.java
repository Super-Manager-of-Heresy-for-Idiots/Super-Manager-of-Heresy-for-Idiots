package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.enums.CombatantType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.response.CharacterAttackResponse;
import com.dnd.app.dto.response.CharacterKnownSpellResponse;
import com.dnd.app.dto.response.CharacterResponse;
import com.dnd.app.dto.response.CombatantTurnResponse;
import com.dnd.app.dto.response.MonsterResponse;
import com.dnd.app.dto.response.TacticalActionResponse;
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
@DisplayName("BattleService.getCurrentTurn: тактические метаданные нацеливания (для превью на карте)")
class BattleServiceTacticalActionsTest {

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
    private final ObjectMapper objectMapper = new ObjectMapper();

    private BattleService battleService;

    private final String username = "gm";
    private final UUID campaignId = UUID.randomUUID();
    private final UUID battleId = UUID.randomUUID();
    private Campaign campaign;
    private Battle battle;
    private User gm;

    @BeforeEach
    void setUp() {
        battleService = new BattleService(battleRepository, combatantRepository, characterRepository,
                userRepository, campaignService, monsterService, characterService,
                characterResourceService, characterEffectService, webSocketEventService,
                diceRoller, weaponAttackService, classAbilityCombatService, itemInstanceRepository,
                spellRepository, spellSlotService, objectMapper);

        gm = User.builder().id(UUID.randomUUID()).username(username).role(Role.ADMIN).build();
        campaign = Campaign.builder().id(campaignId).build();
        battle = Battle.builder()
                .id(battleId).campaign(campaign).status(BattleStatus.ACTIVE)
                .roundNumber(1).currentTurnIndex(0).build();

        lenient().when(userRepository.findByUsername(username)).thenReturn(Optional.of(gm));
        lenient().when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        lenient().when(battleRepository.findByIdAndCampaignId(battleId, campaignId)).thenReturn(Optional.of(battle));
        lenient().when(weaponAttackService.computeAttacks(any())).thenReturn(List.of());
        lenient().when(classAbilityCombatService.classAttacks(any())).thenReturn(List.of());
        lenient().when(classAbilityCombatService.listAbilities(any())).thenReturn(List.of());
    }

    private BattleCombatant characterTurn(PlayerCharacter character) {
        BattleCombatant ch = BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle).type(CombatantType.CHARACTER).character(character)
                .displayName(character.getName()).turnOrder(0).currentHp(30).maxHp(30)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z")).build();
        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId)).thenReturn(List.of(ch));
        return ch;
    }

    private TacticalActionResponse find(List<TacticalActionResponse> actions, String name) {
        return actions.stream().filter(a -> name.equals(a.getName())).findFirst().orElseThrow();
    }

    @Test
    @DisplayName("Оружейная атака: mode=SINGLE_TARGET, требует броска атаки")
    void weaponAttack_isSingleTarget() {
        PlayerCharacter character = PlayerCharacter.builder()
                .id(UUID.randomUUID()).name("Герой").owner(gm).currentHp(30).maxHp(30).build();
        characterTurn(character);

        CharacterResponse resp = CharacterResponse.builder()
                .id(character.getId())
                .attacks(List.of(CharacterAttackResponse.builder()
                        .name("Длинный меч").source("WEAPON")
                        .damage("1к8").damageType("рубящий").range("5 фт.").build()))
                .build();
        when(characterService.getCharacterById(character.getId(), username)).thenReturn(resp);

        CombatantTurnResponse turn = battleService.getCurrentTurn(campaignId, battleId, username);

        TacticalActionResponse sword = find(turn.getTacticalActions(), "Длинный меч");
        assertEquals("WEAPON", sword.getSource());
        assertEquals("SINGLE_TARGET", sword.getTargeting().getMode());
        assertTrue(sword.getTargeting().isRequiresAttackRoll());
        assertFalse(sword.getTargeting().isRequiresSavingThrow());
        // Локализованная строка дальности ненадёжна — rangeFt не выдумываем.
        assertNull(sword.getTargeting().getRangeFt());
        assertEquals("UNKNOWN", sword.getTargeting().getAreaShape());
        assertEquals("1к8", sword.getDamage().get(0).getDice());
    }

    @Test
    @DisplayName("Заклинание со структурной дальностью в футах: rangeFt задан, areaShape=UNKNOWN")
    void spell_exposesStructuredRangeButUnknownArea() {
        PlayerCharacter character = PlayerCharacter.builder()
                .id(UUID.randomUUID()).name("Маг").owner(gm).currentHp(20).maxHp(20).build();
        characterTurn(character);

        UUID spellId = UUID.randomUUID();
        CharacterResponse resp = CharacterResponse.builder()
                .id(character.getId())
                .knownSpells(List.of(CharacterKnownSpellResponse.builder()
                        .spellId(spellId).name("Огненный шар").level(3).build()))
                .build();
        when(characterService.getCharacterById(character.getId(), username)).thenReturn(resp);

        Spell fireball = Spell.builder()
                .id(spellId).slug("fireball").nameRu("Огненный шар").level(3)
                .rangeType("point").rangeDistance(150).rangeUnit("ft")
                .castingActionSlug("action").build();
        when(spellRepository.findAllById(anyList())).thenReturn(List.of(fireball));

        CombatantTurnResponse turn = battleService.getCurrentTurn(campaignId, battleId, username);

        TacticalActionResponse spell = find(turn.getTacticalActions(), "Огненный шар");
        assertEquals("SPELL", spell.getSource());
        assertEquals("ACTION", spell.getActionCost());
        assertEquals(150, spell.getTargeting().getRangeFt());
        // Нет структурных колонок радиуса/формы у заклинаний — всегда UNKNOWN, без парсинга текста.
        assertEquals("UNKNOWN", spell.getTargeting().getAreaShape());
        assertNull(spell.getTargeting().getRadiusFt());
    }

    @Test
    @DisplayName("Заклинание с дальностью 'self': mode=SELF")
    void spell_selfRange_isSelfMode() {
        PlayerCharacter character = PlayerCharacter.builder()
                .id(UUID.randomUUID()).name("Друид").owner(gm).currentHp(20).maxHp(20).build();
        characterTurn(character);

        UUID spellId = UUID.randomUUID();
        CharacterResponse resp = CharacterResponse.builder()
                .id(character.getId())
                .knownSpells(List.of(CharacterKnownSpellResponse.builder()
                        .spellId(spellId).name("Барьерная защита").level(1).build()))
                .build();
        when(characterService.getCharacterById(character.getId(), username)).thenReturn(resp);

        Spell shield = Spell.builder()
                .id(spellId).slug("shield").nameRu("Барьерная защита").level(1)
                .rangeType("self").build();
        when(spellRepository.findAllById(anyList())).thenReturn(List.of(shield));

        CombatantTurnResponse turn = battleService.getCurrentTurn(campaignId, battleId, username);

        TacticalActionResponse spell = find(turn.getTacticalActions(), "Барьерная защита");
        assertEquals("SELF", spell.getTargeting().getMode());
        assertNull(spell.getTargeting().getRangeFt());
        assertEquals("UNKNOWN", spell.getTargeting().getAreaShape());
    }

    @Test
    @DisplayName("Обратная совместимость: character/resources всё ещё заполнены")
    void backwardCompatible_keepsExistingFields() {
        PlayerCharacter character = PlayerCharacter.builder()
                .id(UUID.randomUUID()).name("Боец").owner(gm).currentHp(30).maxHp(30).build();
        characterTurn(character);

        CharacterResponse resp = CharacterResponse.builder().id(character.getId()).build();
        when(characterService.getCharacterById(character.getId(), username)).thenReturn(resp);

        CombatantTurnResponse turn = battleService.getCurrentTurn(campaignId, battleId, username);

        assertNotNull(turn.getCharacter());
        assertNotNull(turn.getTacticalActions());
        assertTrue(turn.getTacticalActions().isEmpty());
    }

    @Test
    @DisplayName("Способности монстра: атака → SINGLE_TARGET с дальностью, спасбросок → requiresSavingThrow")
    void monsterFeatures_exposeReliableTargeting() {
        Monster monster = Monster.builder()
                .id(UUID.randomUUID()).nameRusloc("Дракон").armorClass((short) 18)
                .crValue(new BigDecimal("10")).xpBase(5900).build();
        BattleCombatant mob = BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle).type(CombatantType.MONSTER).monster(monster)
                .displayName("Дракон").turnOrder(0).currentHp(200).maxHp(200)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z")).build();
        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId)).thenReturn(List.of(mob));

        MonsterResponse.FeatureView bite = MonsterResponse.FeatureView.builder()
                .id(UUID.randomUUID()).nameRusloc("Укус").kind("action")
                .attackType("melee").attackBonus((short) 12).reachFt((short) 10)
                .damages(List.of(MonsterResponse.FeatureDamageView.builder().dice("2к10").build()))
                .build();
        MonsterResponse.FeatureView breath = MonsterResponse.FeatureView.builder()
                .id(UUID.randomUUID()).nameRusloc("Огненное дыхание").kind("action")
                .saveDc((short) 19)
                .damages(List.of(MonsterResponse.FeatureDamageView.builder().dice("12к6").build()))
                .build();
        MonsterResponse.FeatureView passive = MonsterResponse.FeatureView.builder()
                .id(UUID.randomUUID()).nameRusloc("Острое чутьё").kind("trait").build();

        MonsterResponse mr = MonsterResponse.builder()
                .id(monster.getId()).nameRusloc("Дракон")
                .features(List.of(bite, breath, passive)).build();
        when(monsterService.getMonster(monster.getId(), username)).thenReturn(mr);

        CombatantTurnResponse turn = battleService.getCurrentTurn(campaignId, battleId, username);

        List<TacticalActionResponse> actions = turn.getTacticalActions();
        assertEquals(2, actions.size(), "пассивная черта без атаки/спасброска не попадает в действия");

        TacticalActionResponse biteA = find(actions, "Укус");
        assertEquals("MONSTER_FEATURE", biteA.getSource());
        assertEquals("SINGLE_TARGET", biteA.getTargeting().getMode());
        assertTrue(biteA.getTargeting().isRequiresAttackRoll());
        assertEquals(10, biteA.getTargeting().getRangeFt());

        TacticalActionResponse breathA = find(actions, "Огненное дыхание");
        assertEquals("UNKNOWN", breathA.getTargeting().getMode());
        assertTrue(breathA.getTargeting().isRequiresSavingThrow());
        assertFalse(breathA.getTargeting().isRequiresAttackRoll());
    }
}
