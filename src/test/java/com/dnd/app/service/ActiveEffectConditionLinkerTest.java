package com.dnd.app.service;

import com.dnd.app.domain.Battle;
import com.dnd.app.domain.BattleCombatant;
import com.dnd.app.domain.Campaign;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.featurerule.ActiveEffectStatus;
import com.dnd.app.domain.featurerule.FeatureActiveEffect;
import com.dnd.app.domain.featurerule.FeatureEffectDefinition;
import com.dnd.app.domain.featurerule.FeatureEffectModifier;
import com.dnd.app.repository.BattleCombatantRepository;
import com.dnd.app.repository.FeatureActiveEffectRepository;
import com.dnd.app.repository.FeatureEffectModifierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Юнит-тесты честной связи эффект→состояние (ABIL §3.1): материализация и снятие. */
@ExtendWith(MockitoExtension.class)
class ActiveEffectConditionLinkerTest {

    private static final String ACTIVE = ActiveEffectStatus.ACTIVE.getCode();

    @Mock private ConditionService conditionService;
    @Mock private FeatureActiveEffectRepository activeRepository;
    @Mock private FeatureEffectModifierRepository modifierRepository;
    @Mock private BattleCombatantRepository battleCombatantRepository;

    private ActiveEffectConditionLinker linker;

    private final UUID defId = UUID.randomUUID();
    private final UUID targetId = UUID.randomUUID();
    private final UUID sourceId = UUID.randomUUID();
    private final UUID conditionId = UUID.randomUUID();
    private final UUID campaignId = UUID.randomUUID();
    private final UUID combatantId = UUID.randomUUID();
    private final UUID instanceId = UUID.randomUUID();
    private final UUID effectId = UUID.randomUUID();

    @BeforeEach
    void setup() {
        linker = new ActiveEffectConditionLinker(conditionService, activeRepository,
                modifierRepository, battleCombatantRepository);
    }

    private FeatureEffectDefinition def() {
        FeatureEffectDefinition def = mock(FeatureEffectDefinition.class);
        lenient().when(def.getId()).thenReturn(defId);
        return def;
    }

    private FeatureEffectModifier conditionModifier() {
        FeatureEffectModifier m = mock(FeatureEffectModifier.class);
        when(m.getConditionId()).thenReturn(conditionId);
        return m;
    }

    private BattleCombatant combatantInActiveBattle() {
        Campaign campaign = mock(Campaign.class);
        when(campaign.getId()).thenReturn(campaignId);
        Battle battle = mock(Battle.class);
        when(battle.getCampaign()).thenReturn(campaign);
        when(battle.getRoundNumber()).thenReturn(3);
        BattleCombatant combatant = mock(BattleCombatant.class);
        when(combatant.getBattle()).thenReturn(battle);
        return combatant;
    }

    @Test
    void materializeReturnsNullWhenNoConditionModifier() {
        when(modifierRepository.findByEffectDefinitionId(defId)).thenReturn(List.of());
        UUID result = linker.materialize(def(), targetId, sourceId, 10);
        assertThat(result).isNull();
        verify(battleCombatantRepository, never()).findByCharacter_IdAndBattle_Status(any(), any());
    }

    @Test
    void materializeReturnsNullWhenTargetNotInActiveBattle() {
        FeatureEffectModifier modifier = conditionModifier();
        when(modifierRepository.findByEffectDefinitionId(defId)).thenReturn(List.of(modifier));
        when(battleCombatantRepository.findByCharacter_IdAndBattle_Status(targetId, BattleStatus.ACTIVE))
                .thenReturn(List.of());
        UUID result = linker.materialize(def(), targetId, sourceId, 10);
        assertThat(result).isNull();
        verify(conditionService, never()).applyForEffect(any(), any(), any(), any(), any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void materializeAppliesConditionAndReturnsInstanceId() {
        BattleCombatant combatant = combatantInActiveBattle();
        FeatureEffectModifier modifier = conditionModifier();
        when(modifierRepository.findByEffectDefinitionId(defId)).thenReturn(List.of(modifier));
        when(battleCombatantRepository.findByCharacter_IdAndBattle_Status(targetId, BattleStatus.ACTIVE))
                .thenReturn(List.of(combatant));
        when(conditionService.applyForEffect(campaignId, combatant, conditionId, 10, sourceId, 3))
                .thenReturn(instanceId);

        UUID result = linker.materialize(def(), targetId, sourceId, 10);

        assertThat(result).isEqualTo(instanceId);
        verify(conditionService).applyForEffect(campaignId, combatant, conditionId, 10, sourceId, 3);
    }

    @Test
    void clearIsNoOpWhenNoLink() {
        FeatureActiveEffect effect = new FeatureActiveEffect();
        effect.setId(effectId);
        effect.setAppliedConditionInstanceId(null);
        linker.clear(effect);
        verify(conditionService, never()).removeByInstanceId(any(), any());
    }

    @Test
    void clearRemovesConditionWhenNoOtherHolder() {
        FeatureActiveEffect effect = new FeatureActiveEffect();
        effect.setId(effectId);
        effect.setSourceCharacterId(sourceId);
        effect.setAppliedConditionInstanceId(instanceId);
        when(activeRepository.countByAppliedConditionInstanceIdAndStatusAndIdNot(instanceId, ACTIVE, effectId))
                .thenReturn(0L);

        linker.clear(effect);

        verify(conditionService).removeByInstanceId(instanceId, sourceId);
        assertThat(effect.getAppliedConditionInstanceId()).isNull();
    }

    @Test
    void clearKeepsConditionWhenAnotherEffectHoldsIt() {
        FeatureActiveEffect effect = new FeatureActiveEffect();
        effect.setId(effectId);
        effect.setSourceCharacterId(sourceId);
        effect.setAppliedConditionInstanceId(instanceId);
        when(activeRepository.countByAppliedConditionInstanceIdAndStatusAndIdNot(instanceId, ACTIVE, effectId))
                .thenReturn(1L);

        linker.clear(effect);

        verify(conditionService, never()).removeByInstanceId(eq(instanceId), any());
        assertThat(effect.getAppliedConditionInstanceId()).isNull();
    }
}
