package com.dnd.app.service;

import com.dnd.app.domain.Battle;
import com.dnd.app.domain.BattleCombatant;
import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.BattleLogType;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.enums.CombatantType;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.featurerule.AvailableFeatureAction;
import com.dnd.app.dto.featurerule.BattleUseAbilityResult;
import com.dnd.app.dto.featurerule.FeatureExecutionPlan;
import com.dnd.app.dto.featurerule.FeatureUseResult;
import com.dnd.app.dto.request.BattleUseAbilityRequest;
import com.dnd.app.repository.BattleCombatantRepository;
import com.dnd.app.repository.BattlePendingResolutionRepository;
import com.dnd.app.repository.BattleRepository;
import com.dnd.app.repository.DamageTypeRepository;
import com.dnd.app.repository.ItemInstanceRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.SpellRepository;
import com.dnd.app.repository.StatTypeRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.service.combat.ClassAbilityCombatService;
import com.dnd.app.service.combat.DiceRoller;
import com.dnd.app.service.combat.WeaponAttackService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("BattleService.useAbility")
class BattleServiceUseAbilityTest {

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
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @Mock private CharacterHpService characterHpService;
    @Mock private ModifierAggregator modifierAggregator;
    @Mock private EffectExpirationService effectExpirationService;
    @Mock private DamageMitigationService damageMitigationService;
    @Mock private ConditionService conditionService;
    @Mock private BattleLogService battleLogService;
    @Mock private SpellCastService spellCastService;
    @Mock private FeatureUseService featureUseService;
    @Mock private ItemAbilityUseService itemAbilityUseService;
    @Mock private CombatFeatureExecutionService combatFeatureExecutionService;
    @Mock private FeatureActionService featureActionService;
    @Mock private StatTypeRepository statTypeRepository;
    @Mock private FeatureEffectService featureEffectService;
    @Mock private BattlePendingResolutionRepository pendingResolutionRepository;
    @Mock private DamageTypeRepository damageTypeRepository;
    @Mock private com.dnd.app.integration.map.MapZoneCreator mapZoneCreator;
    @Mock private com.dnd.app.integration.map.MapTokenMover mapTokenMover;
    @Spy private CommandDedupService commandDedupService = new CommandDedupService();

    @InjectMocks
    private BattleService battleService;

    @BeforeEach
    void injectFieldServices() {
        org.springframework.test.util.ReflectionTestUtils.setField(
                battleService, "featureActionService", featureActionService);
    }

    @Test
    @DisplayName("uses active character ability through feature runtime and writes FEATURE_USE log")
    void useAbility_usesRuntimeAndLogsFeatureUse() {
        UUID campaignId = UUID.randomUUID();
        UUID battleId = UUID.randomUUID();
        UUID characterId = UUID.randomUUID();
        UUID combatantId = UUID.randomUUID();
        UUID featureId = UUID.randomUUID();
        User gm = User.builder().id(UUID.randomUUID()).username("gm").role(Role.ADMIN).build();
        Campaign campaign = Campaign.builder().id(campaignId).build();
        PlayerCharacter character = PlayerCharacter.builder().id(characterId).owner(gm).build();
        Battle battle = Battle.builder()
                .id(battleId)
                .campaign(campaign)
                .status(BattleStatus.ACTIVE)
                .currentTurnIndex(0)
                .roundNumber(1)
                .build();
        BattleCombatant actor = BattleCombatant.builder()
                .id(combatantId)
                .battle(battle)
                .type(CombatantType.CHARACTER)
                .character(character)
                .displayName("Aldar")
                .turnOrder(0)
                .build();
        FeatureExecutionPlan plan = FeatureExecutionPlan.builder()
                .featureId(featureId)
                .featureName("Second Wind")
                .damages(List.of())
                .healings(List.of())
                .resolutions(List.of())
                .attacks(List.of())
                .requiresManualAdjudication(false)
                .build();

        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(gm));
        when(battleRepository.findByIdAndCampaignIdForUpdate(battleId, campaignId)).thenReturn(Optional.of(battle));
        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId)).thenReturn(List.of(actor));
        when(combatFeatureExecutionService.plan(character, featureId)).thenReturn(plan);
        when(featureUseService.use(eq(character), eq(featureId), any())).thenReturn(FeatureUseResult.builder()
                .featureId(featureId)
                .featureName("Second Wind")
                .actionType("bonus_action")
                .resourceKey("second_wind")
                .resourceSpent(1)
                .resourceRemaining(0)
                .message("ok")
                .build());

        BattleUseAbilityResult result = battleService.useAbility(campaignId, battleId,
                BattleUseAbilityRequest.builder().featureId(featureId).build(), "gm");

        assertEquals("USED", result.getOutcome());
        assertEquals(featureId, result.getFeatureId());
        assertEquals("Second Wind", result.getFeatureName());

        ArgumentCaptor<BattleLogType> typeCaptor = ArgumentCaptor.forClass(BattleLogType.class);
        verify(battleLogService).append(eq(battleId), eq(campaignId), typeCaptor.capture(),
                eq(combatantId), eq(null), any(), any(), eq(gm.getId()));
        assertEquals(BattleLogType.FEATURE_USE, typeCaptor.getValue());
        verify(featureUseService).use(eq(character), eq(featureId), any());
    }

    @Test
    @DisplayName("uses item ability through item runtime when itemInstanceId is present")
    void useAbility_usesItemRuntimeWhenItemInstanceIdPresent() {
        UUID campaignId = UUID.randomUUID();
        UUID battleId = UUID.randomUUID();
        UUID characterId = UUID.randomUUID();
        UUID combatantId = UUID.randomUUID();
        UUID ruleId = UUID.randomUUID();
        UUID itemInstanceId = UUID.randomUUID();
        User gm = User.builder().id(UUID.randomUUID()).username("gm").role(Role.ADMIN).build();
        Campaign campaign = Campaign.builder().id(campaignId).build();
        PlayerCharacter character = PlayerCharacter.builder().id(characterId).owner(gm).build();
        Battle battle = Battle.builder()
                .id(battleId)
                .campaign(campaign)
                .status(BattleStatus.ACTIVE)
                .currentTurnIndex(0)
                .roundNumber(1)
                .build();
        BattleCombatant actor = BattleCombatant.builder()
                .id(combatantId)
                .battle(battle)
                .type(CombatantType.CHARACTER)
                .character(character)
                .displayName("Aldar")
                .turnOrder(0)
                .build();
        FeatureExecutionPlan plan = FeatureExecutionPlan.builder()
                .featureId(ruleId)
                .featureName("Wand of Sparks")
                .damages(List.of())
                .healings(List.of())
                .resolutions(List.of())
                .attacks(List.of())
                .requiresManualAdjudication(false)
                .build();

        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(gm));
        when(battleRepository.findByIdAndCampaignIdForUpdate(battleId, campaignId)).thenReturn(Optional.of(battle));
        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId)).thenReturn(List.of(actor));
        when(itemAbilityUseService.plan(character, itemInstanceId, ruleId)).thenReturn(plan);
        when(itemAbilityUseService.use(eq(character), eq(itemInstanceId), eq(ruleId), any())).thenReturn(FeatureUseResult.builder()
                .featureId(ruleId)
                .featureName("Wand of Sparks")
                .actionType("action")
                .resourceKey("charges")
                .resourceSpent(1)
                .resourceRemaining(2)
                .message("ok")
                .build());

        BattleUseAbilityResult result = battleService.useAbility(campaignId, battleId,
                BattleUseAbilityRequest.builder().featureId(ruleId).itemInstanceId(itemInstanceId).build(), "gm");

        assertEquals("USED", result.getOutcome());
        assertEquals(ruleId, result.getFeatureId());
        assertEquals("Wand of Sparks", result.getFeatureName());
        assertEquals(2, result.getResourceRemaining());

        verify(itemAbilityUseService).use(eq(character), eq(itemInstanceId), eq(ruleId), any());
        verify(featureUseService, never()).use(any(), any(), any());
    }

    @Test
    @DisplayName("replays original response for duplicate clientCommandId")
    void useAbility_replaysOriginalResponseForDuplicateCommand() {
        UUID campaignId = UUID.randomUUID();
        UUID battleId = UUID.randomUUID();
        UUID characterId = UUID.randomUUID();
        UUID combatantId = UUID.randomUUID();
        UUID featureId = UUID.randomUUID();
        UUID commandId = UUID.randomUUID();
        User gm = User.builder().id(UUID.randomUUID()).username("gm").role(Role.ADMIN).build();
        Campaign campaign = Campaign.builder().id(campaignId).build();
        PlayerCharacter character = PlayerCharacter.builder().id(characterId).owner(gm).build();
        Battle battle = Battle.builder()
                .id(battleId)
                .campaign(campaign)
                .status(BattleStatus.ACTIVE)
                .currentTurnIndex(0)
                .roundNumber(1)
                .build();
        BattleCombatant actor = BattleCombatant.builder()
                .id(combatantId)
                .battle(battle)
                .type(CombatantType.CHARACTER)
                .character(character)
                .displayName("Aldar")
                .turnOrder(0)
                .build();
        FeatureExecutionPlan plan = FeatureExecutionPlan.builder()
                .featureId(featureId)
                .featureName("Second Wind")
                .damages(List.of())
                .healings(List.of())
                .resolutions(List.of())
                .attacks(List.of())
                .requiresManualAdjudication(false)
                .build();

        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(gm));
        when(battleRepository.findByIdAndCampaignIdForUpdate(battleId, campaignId)).thenReturn(Optional.of(battle));
        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId)).thenReturn(List.of(actor));
        when(combatFeatureExecutionService.plan(character, featureId)).thenReturn(plan);
        when(featureUseService.use(eq(character), eq(featureId), any())).thenReturn(FeatureUseResult.builder()
                .featureId(featureId)
                .featureName("Second Wind")
                .actionType("bonus_action")
                .resourceKey("second_wind")
                .resourceSpent(1)
                .resourceRemaining(0)
                .message("ok")
                .build());

        BattleUseAbilityRequest request = BattleUseAbilityRequest.builder()
                .featureId(featureId)
                .clientCommandId(commandId)
                .build();

        BattleUseAbilityResult first = battleService.useAbility(campaignId, battleId, request, "gm");
        BattleUseAbilityResult second = battleService.useAbility(campaignId, battleId, request, "gm");

        assertEquals("USED", first.getOutcome());
        assertEquals("USED", second.getOutcome());
        assertEquals(first.getFeatureName(), second.getFeatureName());
        assertEquals(first.getResourceRemaining(), second.getResourceRemaining());
        verify(featureUseService, times(1)).use(eq(character), eq(featureId), any());
    }

    @Test
    @DisplayName("manual-required ability does not spend feature runtime")
    void useAbility_manualRequiredDoesNotSpend() {
        UUID campaignId = UUID.randomUUID();
        UUID battleId = UUID.randomUUID();
        UUID characterId = UUID.randomUUID();
        UUID combatantId = UUID.randomUUID();
        UUID featureId = UUID.randomUUID();
        User gm = User.builder().id(UUID.randomUUID()).username("gm").role(Role.ADMIN).build();
        Campaign campaign = Campaign.builder().id(campaignId).build();
        PlayerCharacter character = PlayerCharacter.builder().id(characterId).owner(gm).build();
        Battle battle = Battle.builder()
                .id(battleId)
                .campaign(campaign)
                .status(BattleStatus.ACTIVE)
                .currentTurnIndex(0)
                .roundNumber(1)
                .build();
        BattleCombatant actor = BattleCombatant.builder()
                .id(combatantId)
                .battle(battle)
                .type(CombatantType.CHARACTER)
                .character(character)
                .displayName("Aldar")
                .turnOrder(0)
                .build();
        FeatureExecutionPlan plan = FeatureExecutionPlan.builder()
                .featureId(featureId)
                .featureName("Indomitable")
                .damages(List.of())
                .healings(List.of())
                .resolutions(List.of())
                .attacks(List.of())
                .requiresManualAdjudication(true)
                .build();

        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(gm));
        when(battleRepository.findByIdAndCampaignIdForUpdate(battleId, campaignId)).thenReturn(Optional.of(battle));
        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId)).thenReturn(List.of(actor));
        when(combatFeatureExecutionService.plan(character, featureId)).thenReturn(plan);

        BattleUseAbilityResult result = battleService.useAbility(campaignId, battleId,
                BattleUseAbilityRequest.builder().featureId(featureId).build(), "gm");

        assertEquals("MANUAL_REQUIRED", result.getOutcome());
        verify(featureUseService, never()).use(any(), any(), any());
        verify(itemAbilityUseService, never()).use(any(), any(), any(), any());
    }

    @Test
    @DisplayName("off-turn class ability is allowed only through reaction action cost")
    void useAbility_allowsOffTurnReactionAbility() {
        UUID campaignId = UUID.randomUUID();
        UUID battleId = UUID.randomUUID();
        UUID activeCharacterId = UUID.randomUUID();
        UUID reactorCharacterId = UUID.randomUUID();
        UUID activeCombatantId = UUID.randomUUID();
        UUID reactorCombatantId = UUID.randomUUID();
        UUID featureId = UUID.randomUUID();
        User gm = User.builder().id(UUID.randomUUID()).username("gm").role(Role.ADMIN).build();
        Campaign campaign = Campaign.builder().id(campaignId).build();
        PlayerCharacter activeCharacter = PlayerCharacter.builder().id(activeCharacterId).owner(gm).build();
        PlayerCharacter reactorCharacter = PlayerCharacter.builder().id(reactorCharacterId).owner(gm).build();
        Battle battle = Battle.builder()
                .id(battleId)
                .campaign(campaign)
                .status(BattleStatus.ACTIVE)
                .currentTurnIndex(0)
                .roundNumber(1)
                .build();
        BattleCombatant active = BattleCombatant.builder()
                .id(activeCombatantId)
                .battle(battle)
                .type(CombatantType.CHARACTER)
                .character(activeCharacter)
                .displayName("Active")
                .turnOrder(0)
                .build();
        BattleCombatant reactor = BattleCombatant.builder()
                .id(reactorCombatantId)
                .battle(battle)
                .type(CombatantType.CHARACTER)
                .character(reactorCharacter)
                .displayName("Reactor")
                .turnOrder(1)
                .build();
        FeatureExecutionPlan plan = FeatureExecutionPlan.builder()
                .featureId(featureId)
                .featureName("Cutting Words")
                .damages(List.of())
                .healings(List.of())
                .resolutions(List.of())
                .attacks(List.of())
                .requiresManualAdjudication(false)
                .build();

        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(gm));
        when(battleRepository.findByIdAndCampaignIdForUpdate(battleId, campaignId)).thenReturn(Optional.of(battle));
        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId)).thenReturn(List.of(active, reactor));
        when(featureActionService.listAvailableActions(reactorCharacter, battleId)).thenReturn(List.of(
                AvailableFeatureAction.builder()
                        .featureId(featureId)
                        .featureName("Cutting Words")
                        .actionType("reaction")
                        .available(true)
                        .hasExecutableRules(true)
                        .manualOnly(false)
                        .build()));
        when(combatFeatureExecutionService.plan(reactorCharacter, featureId)).thenReturn(plan);
        when(featureUseService.use(eq(reactorCharacter), eq(featureId), any())).thenReturn(FeatureUseResult.builder()
                .featureId(featureId)
                .featureName("Cutting Words")
                .actionType("reaction")
                .message("ok")
                .build());

        BattleUseAbilityResult result = battleService.useAbility(campaignId, battleId,
                BattleUseAbilityRequest.builder().featureId(featureId).combatantId(reactorCombatantId).build(), "gm");

        assertEquals("USED", result.getOutcome());
        verify(featureUseService).use(eq(reactorCharacter), eq(featureId), any());
    }

    @Test
    @DisplayName("plans current-turn character ability without spending")
    void planAbility_returnsCurrentTurnPlan() {
        UUID campaignId = UUID.randomUUID();
        UUID battleId = UUID.randomUUID();
        UUID characterId = UUID.randomUUID();
        UUID combatantId = UUID.randomUUID();
        UUID featureId = UUID.randomUUID();
        User gm = User.builder().id(UUID.randomUUID()).username("gm").role(Role.ADMIN).build();
        Campaign campaign = Campaign.builder().id(campaignId).build();
        PlayerCharacter character = PlayerCharacter.builder().id(characterId).owner(gm).build();
        Battle battle = Battle.builder()
                .id(battleId)
                .campaign(campaign)
                .status(BattleStatus.ACTIVE)
                .currentTurnIndex(0)
                .roundNumber(1)
                .build();
        BattleCombatant actor = BattleCombatant.builder()
                .id(combatantId)
                .battle(battle)
                .type(CombatantType.CHARACTER)
                .character(character)
                .displayName("Aldar")
                .turnOrder(0)
                .build();
        FeatureExecutionPlan plan = FeatureExecutionPlan.builder()
                .featureId(featureId)
                .featureName("Second Wind")
                .damages(List.of())
                .healings(List.of())
                .resolutions(List.of())
                .attacks(List.of())
                .requiresManualAdjudication(false)
                .build();

        when(userRepository.findByUsername("gm")).thenReturn(Optional.of(gm));
        when(campaignService.findCampaign(campaignId)).thenReturn(campaign);
        when(battleRepository.findByIdAndCampaignId(battleId, campaignId)).thenReturn(Optional.of(battle));
        when(combatantRepository.findByBattleIdOrderByTurnOrderAsc(battleId)).thenReturn(List.of(actor));
        when(combatFeatureExecutionService.plan(character, featureId)).thenReturn(plan);

        FeatureExecutionPlan result = battleService.planAbility(campaignId, battleId, featureId, "gm");

        assertEquals(featureId, result.getFeatureId());
        verify(featureUseService, never()).use(any(), any(), any());
    }
}
