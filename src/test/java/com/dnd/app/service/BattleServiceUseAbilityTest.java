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
import com.dnd.app.dto.featurerule.BattleUseAbilityResult;
import com.dnd.app.dto.featurerule.FeatureExecutionPlan;
import com.dnd.app.dto.featurerule.FeatureUseResult;
import com.dnd.app.dto.request.BattleUseAbilityRequest;
import com.dnd.app.repository.BattleCombatantRepository;
import com.dnd.app.repository.BattleRepository;
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
    @Mock private StatTypeRepository statTypeRepository;
    @Mock private FeatureEffectService featureEffectService;
    @Mock private com.dnd.app.integration.map.MapZoneCreator mapZoneCreator;
    @Mock private com.dnd.app.integration.map.MapTokenMover mapTokenMover;
    @Spy private CommandDedupService commandDedupService = new CommandDedupService();

    @InjectMocks
    private BattleService battleService;

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
}
