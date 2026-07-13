package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.enums.CombatantType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.response.BattleAccessResponse;
import com.dnd.app.dto.response.CombatantReferenceResponse;
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

@ExtendWith(MockitoExtension.class)
@DisplayName("BattleService: контракты интеграции с map-service (доступ и ссылки на бойцов)")
class BattleServiceMapSyncTest {

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

    private final UUID campaignId = UUID.randomUUID();
    private final UUID battleId = UUID.randomUUID();
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
                modifierAggregator, effectExpirationService,
                new DamageMitigationService(modifierAggregator),
                org.mockito.Mockito.mock(ConditionService.class),
                org.mockito.Mockito.mock(BattleLogService.class),
                org.mockito.Mockito.mock(SpellCastService.class),
                org.mockito.Mockito.mock(FeatureUseService.class),
                org.mockito.Mockito.mock(ItemAbilityUseService.class),
                org.mockito.Mockito.mock(CombatFeatureExecutionService.class),
                org.mockito.Mockito.mock(StatTypeRepository.class),
                org.mockito.Mockito.mock(FeatureEffectService.class),
                org.mockito.Mockito.mock(com.dnd.app.integration.map.MapZoneCreator.class), org.mockito.Mockito.mock(com.dnd.app.integration.map.MapTokenMover.class), new com.dnd.app.service.CommandDedupService());
        campaign = Campaign.builder().id(campaignId).build();
        battle = Battle.builder()
                .id(battleId).campaign(campaign).status(BattleStatus.ACTIVE)
                .roundNumber(1).currentTurnIndex(0).build();
        lenient().when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        lenient().when(battleRepository.findByIdAndCampaignId(battleId, campaignId)).thenReturn(Optional.of(battle));
    }

    private BattleCombatant monsterCombatant() {
        Monster monster = Monster.builder()
                .id(UUID.randomUUID()).nameRusloc("Гоблин").armorClass((short) 15)
                .crValue(new BigDecimal("1")).xpBase(100).build();
        return BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle).type(CombatantType.MONSTER).monster(monster)
                .displayName("Гоблин #1").turnOrder(1).currentHp(7).maxHp(7)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z")).build();
    }

    private BattleCombatant characterCombatant(User owner) {
        PlayerCharacter character = PlayerCharacter.builder()
                .id(UUID.randomUUID()).name("Таргим").owner(owner)
                .currentHp(24).maxHp(31).build();
        return BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle).type(CombatantType.CHARACTER).character(character)
                .displayName("Таргим").turnOrder(0).currentHp(24).maxHp(31)
                .createdAt(Instant.parse("2026-01-01T00:00:01Z")).build();
    }

    @Test
    @DisplayName("Доступ: мастер управляет битвой и любым бойцом")
    void access_gmControlsEverything() {
        User gm = User.builder().id(UUID.randomUUID()).username("gm").role(Role.ADMIN).build();
        User player = User.builder().id(UUID.randomUUID()).username("p").role(Role.PLAYER).build();
        BattleCombatant ch = characterCombatant(player);
        BattleCombatant mob = monsterCombatant();

        when(userRepository.findById(gm.getId())).thenReturn(Optional.of(gm));
        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId)).thenReturn(List.of(ch, mob));

        BattleAccessResponse access = battleService.getBattleAccess(campaignId, battleId, gm.getId());

        assertTrue(access.isCanView());
        assertTrue(access.isCanManageBattle());
        assertTrue(access.isCanControlAnyCombatant());
        assertEquals(2, access.getControllableCombatantIds().size(), "мастер контролирует всех бойцов");
        assertEquals(List.of(ch.getCharacter().getId()), access.getControllableCharacterIds());
    }

    @Test
    @DisplayName("Доступ: игрок видит битву, но контролирует только своего персонажа")
    void access_playerControlsOwnedOnly() {
        User player = User.builder().id(UUID.randomUUID()).username("p").role(Role.PLAYER).build();
        User other = User.builder().id(UUID.randomUUID()).username("o").role(Role.PLAYER).build();
        BattleCombatant mine = characterCombatant(player);
        BattleCombatant theirs = characterCombatant(other);
        BattleCombatant mob = monsterCombatant();

        when(userRepository.findById(player.getId())).thenReturn(Optional.of(player));
        when(campaignService.isGmInCampaign(campaignId, player.getId())).thenReturn(false);
        when(campaignService.isMemberOfCampaign(campaignId, player.getId())).thenReturn(true);
        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId))
                .thenReturn(List.of(mine, theirs, mob));

        BattleAccessResponse access = battleService.getBattleAccess(campaignId, battleId, player.getId());

        assertTrue(access.isCanView());
        assertFalse(access.isCanManageBattle());
        assertFalse(access.isCanControlAnyCombatant());
        assertEquals(List.of(mine.getId()), access.getControllableCombatantIds());
        assertEquals(List.of(mine.getCharacter().getId()), access.getControllableCharacterIds());
    }

    @Test
    @DisplayName("Доступ: не-участник кампании не видит битву")
    void access_nonMemberCannotView() {
        User stranger = User.builder().id(UUID.randomUUID()).username("s").role(Role.PLAYER).build();

        when(userRepository.findById(stranger.getId())).thenReturn(Optional.of(stranger));
        when(campaignService.isGmInCampaign(campaignId, stranger.getId())).thenReturn(false);
        when(campaignService.isMemberOfCampaign(campaignId, stranger.getId())).thenReturn(false);

        BattleAccessResponse access = battleService.getBattleAccess(campaignId, battleId, stranger.getId());

        assertFalse(access.isCanView());
        assertFalse(access.isCanManageBattle());
        assertTrue(access.getControllableCombatantIds().isEmpty());
        assertTrue(access.getControllableCharacterIds().isEmpty());
        verify(combatantRepository, never()).findByBattleIdOrderByTurnOrderAsc(any());
    }

    @Test
    @DisplayName("Ссылка на персонажа: содержит combatantId, characterId, ownerUserId")
    void reference_character() {
        User player = User.builder().id(UUID.randomUUID()).username("p").role(Role.PLAYER).build();
        BattleCombatant ch = characterCombatant(player);
        when(combatantRepository.findById(ch.getId())).thenReturn(Optional.of(ch));

        CombatantReferenceResponse ref = battleService.getCombatantReference(campaignId, battleId, ch.getId());

        assertEquals(ch.getId(), ref.getCombatantId());
        assertEquals("CHARACTER", ref.getType());
        assertEquals(ch.getCharacter().getId(), ref.getCharacterId());
        assertEquals(player.getId(), ref.getOwnerUserId());
        assertNull(ref.getMonsterId());
        assertTrue(ref.isCurrentTurn(), "turnOrder 0 == currentTurnIndex 0 в активной битве");
        assertEquals(1, ref.getWidthCells());
        assertEquals(1, ref.getHeightCells());
    }

    @Test
    @DisplayName("Ссылка на монстра: содержит combatantId, monsterId, displayName и без приватных данных")
    void reference_monster() {
        BattleCombatant mob = monsterCombatant();
        when(combatantRepository.findById(mob.getId())).thenReturn(Optional.of(mob));

        CombatantReferenceResponse ref = battleService.getCombatantReference(campaignId, battleId, mob.getId());

        assertEquals(mob.getId(), ref.getCombatantId());
        assertEquals("MONSTER", ref.getType());
        assertEquals(mob.getMonster().getId(), ref.getMonsterId());
        assertEquals("Гоблин #1", ref.getDisplayName());
        assertNull(ref.getCharacterId());
        assertNull(ref.getOwnerUserId());
        assertFalse(ref.isCurrentTurn(), "turnOrder 1 != currentTurnIndex 0");
    }
}
