package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.enums.CombatantType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.BattleAttackRequest;
import com.dnd.app.dto.request.BattleUseItemRequest;
import com.dnd.app.dto.request.JoinBattleRequest;
import com.dnd.app.dto.request.SpendActionRequest;
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
@DisplayName("BattleService: устойчивость боевых мутаций к гонкам (блокировки строк + отказ повторных действий)")
class BattleServiceConsistencyTest {

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
    private User gm;
    private Campaign campaign;
    private Battle battle;

    @BeforeEach
    void setUp() {
        battleService = new BattleService(battleRepository, combatantRepository, characterRepository,
                userRepository, campaignService, monsterService, characterService,
                characterResourceService, characterEffectService, webSocketEventService,
                diceRoller, weaponAttackService, classAbilityCombatService, itemInstanceRepository,
                spellRepository, spellSlotService, objectMapper,
                new CharacterHpService(characterRepository, combatantRepository,
                        webSocketEventService, gameplayEventService),
                modifierAggregator, effectExpirationService);

        gm = User.builder().id(UUID.randomUUID()).username(username).role(Role.ADMIN).build();
        campaign = Campaign.builder().id(campaignId).build();
        battle = Battle.builder()
                .id(battleId).campaign(campaign).status(BattleStatus.ACTIVE)
                .roundNumber(1).currentTurnIndex(0).build();

        lenient().when(userRepository.findByUsername(username)).thenReturn(Optional.of(gm));
        lenient().when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        lenient().when(battleRepository.findByIdAndCampaignIdForUpdate(battleId, campaignId))
                .thenReturn(Optional.of(battle));
    }

    private BattleCombatant monsterCombatant(boolean actionUsed) {
        FeatureDamage dmg = FeatureDamage.builder().id(UUID.randomUUID()).sortOrder(0).dice("3к4").build();
        MonsterFeature bite = MonsterFeature.builder()
                .id(UUID.randomUUID()).section("actions").sortOrder(1).kind("action")
                .nameRusloc("Укус").attackType("melee").attackBonus((short) 5)
                .damages(new java.util.ArrayList<>(List.of(dmg))).descriptionRusloc("bite").build();
        Monster monster = Monster.builder()
                .id(UUID.randomUUID()).nameRusloc("Гоблин").armorClass((short) 15)
                .crValue(new BigDecimal("1")).xpBase(100)
                .features(new java.util.ArrayList<>(List.of(bite))).build();
        return BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle).type(CombatantType.MONSTER).monster(monster)
                .displayName("Гоблин #1").turnOrder(0).currentHp(7).maxHp(7).actionSpent(actionUsed ? 1 : 0)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z")).build();
    }

    private BattleCombatant characterCombatant(int turnOrder, boolean actionUsed) {
        PlayerCharacter character = PlayerCharacter.builder()
                .id(UUID.randomUUID()).name("Герой").owner(gm)
                .currentHp(20).maxHp(20).tempHp(0).armorClass(12).build();
        return BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle).type(CombatantType.CHARACTER).character(character)
                .displayName("Герой").turnOrder(turnOrder).currentHp(20).maxHp(20).actionSpent(actionUsed ? 1 : 0)
                .createdAt(Instant.parse("2026-01-01T00:00:0" + turnOrder + "Z")).build();
    }

    @Test
    @DisplayName("Повторная атака тем же бойцом не тратит действие дважды (отклоняется)")
    void duplicateAttack_rejected() {
        BattleCombatant attacker = monsterCombatant(true); // действие уже потрачено
        BattleCombatant target = characterCombatant(1, false);
        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId))
                .thenReturn(List.of(attacker, target));
        when(combatantRepository.findByIdForUpdate(attacker.getId())).thenReturn(Optional.of(attacker));

        BattleAttackRequest req = BattleAttackRequest.builder()
                .targetCombatantId(target.getId()).attackName("Укус").d20(15).build();

        assertThrows(BadRequestException.class,
                () -> battleService.performAttack(campaignId, battleId, req, username));

        // Блокировки строк действительно используются для боевой мутации.
        verify(battleRepository).findByIdAndCampaignIdForUpdate(battleId, campaignId);
        verify(combatantRepository).findByIdForUpdate(attacker.getId());
        // Цель не трогаем и действие не перезаписываем.
        verify(combatantRepository, never()).save(any());
    }

    @Test
    @DisplayName("Успешная атака блокирует строки атакующего и цели (детерминированно для HP монстра)")
    void attack_locksAttackerAndTargetRows() {
        BattleCombatant attacker = monsterCombatant(false);
        PlayerCharacter character = PlayerCharacter.builder()
                .id(UUID.randomUUID()).name("Жертва").owner(gm)
                .currentHp(20).maxHp(20).tempHp(0).armorClass(12).build();
        BattleCombatant target = BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle).type(CombatantType.CHARACTER).character(character)
                .displayName("Жертва").turnOrder(1).currentHp(20).maxHp(20)
                .createdAt(Instant.parse("2026-01-01T00:00:01Z")).build();

        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId))
                .thenReturn(List.of(attacker, target));
        when(combatantRepository.findByIdForUpdate(attacker.getId())).thenReturn(Optional.of(attacker));
        when(combatantRepository.findByIdForUpdate(target.getId())).thenReturn(Optional.of(target));
        when(characterRepository.findByIdForUpdate(character.getId())).thenReturn(Optional.of(character));
        when(diceRoller.rollDamage("3к4", false)).thenReturn(5);

        BattleAttackRequest req = BattleAttackRequest.builder()
                .targetCombatantId(target.getId()).attackName("Укус").d20(15).build();

        battleService.performAttack(campaignId, battleId, req, username);

        verify(combatantRepository).findByIdForUpdate(attacker.getId());
        verify(combatantRepository).findByIdForUpdate(target.getId());
        assertTrue(attacker.getActionSpent() >= attacker.getActionMax(), "действие атакующего помечается потраченным");
    }

    @Test
    @DisplayName("Повторное использование предмета не тратит действие дважды (отклоняется)")
    void duplicateUseItem_rejected() {
        BattleCombatant actor = characterCombatant(0, true); // действие уже потрачено
        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId)).thenReturn(List.of(actor));
        when(combatantRepository.findByIdForUpdate(actor.getId())).thenReturn(Optional.of(actor));

        BattleUseItemRequest req = BattleUseItemRequest.builder()
                .itemInstanceId(UUID.randomUUID()).build();

        assertThrows(BadRequestException.class,
                () -> battleService.performUseItem(campaignId, battleId, req, username));

        // Предмет даже не загружается, если действие уже потрачено.
        verify(itemInstanceRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("spendAction отклоняет повторную трату одного и того же слота действия")
    void spendAction_rejectsDuplicate() {
        BattleCombatant combatant = characterCombatant(0, true); // ACTION уже потрачено
        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId)).thenReturn(List.of(combatant));
        when(combatantRepository.findByIdForUpdate(combatant.getId())).thenReturn(Optional.of(combatant));

        SpendActionRequest req = SpendActionRequest.builder().slot(SpendActionRequest.Slot.ACTION).build();

        assertThrows(BadRequestException.class,
                () -> battleService.spendAction(campaignId, battleId, combatant.getId(), req, username));

        verify(combatantRepository).findByIdForUpdate(combatant.getId());
    }

    @Test
    @DisplayName("startBattle блокирует строку битвы (consistent lock order)")
    void startBattle_locksBattleRow() {
        battle.setStatus(BattleStatus.ASSEMBLING);
        Monster monster = Monster.builder()
                .id(UUID.randomUUID()).nameRusloc("Гоблин").armorClass((short) 15)
                .crValue(new BigDecimal("1")).xpBase(100).build();
        BattleCombatant mob = BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle).type(CombatantType.MONSTER).monster(monster)
                .displayName("Гоблин #1").turnOrder(0).currentHp(7).maxHp(7)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z")).build();
        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId))
                .thenReturn(new java.util.ArrayList<>(List.of(mob)));
        when(diceRoller.rollD20()).thenReturn(10);

        battleService.startBattle(campaignId, battleId, username);

        verify(battleRepository).findByIdAndCampaignIdForUpdate(battleId, campaignId);
        verify(battleRepository, never()).findByIdAndCampaignId(any(), any());
    }

    @Test
    @DisplayName("joinCharacters не допускает повторное добавление того же персонажа")
    void joinCharacters_rejectsDuplicateCharacter() {
        UUID characterId = UUID.randomUUID();
        PlayerCharacter character = PlayerCharacter.builder()
                .id(characterId).name("Герой").owner(gm).campaign(campaign).currentHp(20).maxHp(20).build();

        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId)).thenReturn(List.of());
        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character));
        when(combatantRepository.existsByBattleIdAndCharacterId(battleId, characterId)).thenReturn(true);

        JoinBattleRequest req = JoinBattleRequest.builder()
                .characters(List.of(JoinBattleRequest.CharacterJoin.builder()
                        .characterId(characterId).d20(10).build()))
                .build();

        assertThrows(BadRequestException.class,
                () -> battleService.joinCharacters(campaignId, battleId, req, username));

        verify(battleRepository).findByIdAndCampaignIdForUpdate(battleId, campaignId);
        verify(combatantRepository, never()).save(any());
    }
}
