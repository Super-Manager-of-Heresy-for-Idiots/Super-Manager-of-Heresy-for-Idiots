package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.enums.CharacterStatus;
import com.dnd.app.domain.enums.CombatantType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.response.BattleResponse;
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

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("BattleService: спасброски от смерти (1.3)")
class BattleServiceDeathSaveTest {

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

    private PlayerCharacter character;
    private BattleCombatant combatant;

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
                mock(ConditionService.class),
                mock(BattleLogService.class),
                mock(SpellCastService.class),
                mock(StatTypeRepository.class),
                mock(FeatureEffectService.class));

        User gm = User.builder().id(UUID.randomUUID()).username(username).role(Role.ADMIN).build();
        User owner = User.builder().id(UUID.randomUUID()).username("player").role(Role.PLAYER).build();
        Campaign campaign = Campaign.builder().id(campaignId).build();
        Battle battle = Battle.builder().id(battleId).campaign(campaign).status(BattleStatus.ACTIVE)
                .roundNumber(1).currentTurnIndex(0).build();

        character = PlayerCharacter.builder()
                .id(UUID.randomUUID()).name("Плут").owner(owner).status(CharacterStatus.ACTIVE)
                .currentHp(0).maxHp(30).deathSaveSuccesses(0).deathSaveFailures(0).build();
        combatant = BattleCombatant.builder()
                .id(UUID.randomUUID()).battle(battle).type(CombatantType.CHARACTER).character(character)
                .displayName("Плут").turnOrder(0).currentHp(0).maxHp(30)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z")).build();

        lenient().when(userRepository.findByUsername(username)).thenReturn(Optional.of(gm));
        lenient().when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        lenient().when(battleRepository.findByIdAndCampaignIdForUpdate(battleId, campaignId)).thenReturn(Optional.of(battle));
        lenient().when(combatantRepository.findByIdForUpdate(combatant.getId())).thenReturn(Optional.of(combatant));
        lenient().when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId)).thenReturn(List.of(combatant));
        lenient().when(combatantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(characterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private BattleResponse deathSave(Integer roll) {
        return battleService.rollDeathSave(campaignId, battleId, combatant.getId(), roll, username);
    }

    @Test
    @DisplayName("10+ → успех")
    void success_addsSuccess() {
        deathSave(15);
        assertEquals(1, character.getDeathSaveSuccesses());
        assertEquals(0, character.getDeathSaveFailures());
    }

    @Test
    @DisplayName("<10 → провал")
    void failure_addsFailure() {
        deathSave(5);
        assertEquals(1, character.getDeathSaveFailures());
    }

    @Test
    @DisplayName("nat1 → два провала")
    void nat1_addsTwoFailures() {
        deathSave(1);
        assertEquals(2, character.getDeathSaveFailures());
        assertEquals(CharacterStatus.ACTIVE, character.getStatus());
    }

    @Test
    @DisplayName("Третий провал → смерть")
    void thirdFailure_kills() {
        character.setDeathSaveFailures(2);
        deathSave(5);
        assertEquals(3, character.getDeathSaveFailures());
        assertEquals(CharacterStatus.DEAD, character.getStatus());
    }

    @Test
    @DisplayName("Третий успех → стабилизация (счётчики сброшены)")
    void thirdSuccess_stabilizes() {
        character.setDeathSaveSuccesses(2);
        deathSave(12);
        assertEquals(0, character.getDeathSaveSuccesses());
        assertEquals(0, character.getDeathSaveFailures());
        assertEquals(CharacterStatus.ACTIVE, character.getStatus());
    }

    @Test
    @DisplayName("Стабилизация вручную сбрасывает счётчики")
    void stabilize_clearsCounters() {
        character.setDeathSaveFailures(2);
        battleService.stabilize(campaignId, battleId, combatant.getId(), username);
        assertEquals(0, character.getDeathSaveFailures());
    }

    @Test
    @DisplayName("Спасбросок только у умирающего (0 HP): у здорового — ошибка")
    void notDying_rejected() {
        combatant.setCurrentHp(20);
        character.setCurrentHp(20);
        assertThrows(com.dnd.app.exception.BadRequestException.class, () -> deathSave(15));
    }
}
