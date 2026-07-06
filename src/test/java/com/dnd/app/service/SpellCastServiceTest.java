package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.Spell;
import com.dnd.app.domain.featurerule.ActionType;
import com.dnd.app.domain.featurerule.FeatureActionCost;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.dto.featurerule.FeatureExecutionPlan;
import com.dnd.app.dto.featurerule.SpellCastRequest;
import com.dnd.app.dto.featurerule.SpellCastResult;
import com.dnd.app.dto.response.SpellSlotsResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.ActionTypeRepository;
import com.dnd.app.repository.CharacterKnownSpellRepository;
import com.dnd.app.repository.FeatureActionCostRepository;
import com.dnd.app.repository.FeatureUseLogRepository;
import com.dnd.app.repository.SpellRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SpellCastServiceTest {

    @Mock private FeatureRulesProperties flags;
    @Mock private SpellRepository spellRepository;
    @Mock private CharacterKnownSpellRepository knownSpellRepository;
    @Mock private CharacterFeatureResolver resolver;
    @Mock private FeatureActionCostRepository actionCostRepository;
    @Mock private ActionTypeRepository actionTypeRepository;
    @Mock private CombatActionEconomyService economyService;
    @Mock private SpellSlotService slotService;
    @Mock private CombatFeatureExecutionService executionService;
    @Mock private FeatureEffectService effectService;
    @Mock private GameplayEventService gameplayEventService;
    @Mock private FeatureUseLogRepository useLogRepository;

    @InjectMocks private SpellCastService service;

    private final UUID spellId = UUID.randomUUID();
    private final PlayerCharacter caster = PlayerCharacter.builder().id(UUID.randomUUID()).build();

    private Spell spell(int level) {
        return Spell.builder().id(spellId).slug("fireball").nameRu("Огненный шар").level(level).build();
    }

    @BeforeEach
    void stubCommon() {
        lenient().when(useLogRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(executionService.planForSpell(any(), any(), any()))
                .thenReturn(FeatureExecutionPlan.builder().featureId(spellId).build());
        lenient().when(effectService.applyForSpellCast(any(), any(), any())).thenReturn(0);
    }

    @Test
    void castIsRejectedWhenSpellsRuntimeDisabled() {
        when(flags.spellsActive()).thenReturn(false);
        assertThatThrownBy(() -> service.cast(caster, spellId, null, null))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void castIsRejectedWhenCharacterDoesNotKnowTheSpell() {
        when(flags.spellsActive()).thenReturn(true);
        when(spellRepository.findById(spellId)).thenReturn(Optional.of(spell(3)));
        when(knownSpellRepository.existsByCharacterIdAndSpellId(caster.getId(), spellId)).thenReturn(false);

        assertThatThrownBy(() -> service.cast(caster, spellId, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("не знает");
        verify(slotService, never()).expendInternal(any(), anyInt());
    }

    @Test
    void leveledSpellSpendsSlotOfItsLevelByDefault() {
        knownSpell(3);
        when(slotService.expendInternal(caster.getId(), 3))
                .thenReturn(SpellSlotsResponse.builder().build());

        SpellCastResult result = service.cast(caster, spellId, null, null);

        assertThat(result.getSlotLevelUsed()).isEqualTo(3);
        verify(slotService).expendInternal(caster.getId(), 3);
        verify(executionService).planForSpell(caster, spellAsLoaded(), 3);
        verify(effectService).applyForSpellCast(caster, caster, spellId);
    }

    @Test
    void cantripNeverSpendsASlot() {
        knownSpell(0);

        SpellCastResult result = service.cast(caster, spellId, null, null);

        assertThat(result.getSlotLevelUsed()).isNull();
        verify(slotService, never()).expendInternal(any(), anyInt());
        verify(executionService).planForSpell(caster, spellAsLoaded(), null);
    }

    @Test
    void upcastingBelowSpellLevelIsRejectedBeforeAnySpend() {
        knownSpell(3);
        SpellCastRequest request = SpellCastRequest.builder().slotLevel(2).build();

        assertThatThrownBy(() -> service.cast(caster, spellId, request, null))
                .isInstanceOf(BadRequestException.class);
        verify(slotService, never()).expendInternal(any(), anyInt());
    }

    @Test
    void combatCastSpendsTheBackfilledActionCostBeforeTheSlot() {
        knownSpell(3);
        UUID combatId = UUID.randomUUID();
        FeatureRule costRule = FeatureRule.builder().id(UUID.randomUUID()).build();
        when(resolver.approvedEnabledRules(eq(FeatureRuleOwnerType.SPELL), anyList()))
                .thenReturn(List.of(costRule));
        UUID actionTypeId = UUID.randomUUID();
        when(actionCostRepository.findByFeatureRuleIdIn(anyList())).thenReturn(List.of(
                FeatureActionCost.builder().featureRuleId(costRule.getId()).actionTypeId(actionTypeId).build()));
        when(actionTypeRepository.findById(actionTypeId))
                .thenReturn(Optional.of(ActionType.builder().id(actionTypeId).code("bonus_action").build()));
        when(slotService.expendInternal(caster.getId(), 3))
                .thenReturn(SpellSlotsResponse.builder().build());

        SpellCastResult result = service.cast(caster, spellId,
                SpellCastRequest.builder().combatId(combatId).build(), null);

        assertThat(result.getActionType()).isEqualTo("bonus_action");
        verify(economyService).spend(combatId, caster.getId(), "bonus_action");
    }

    @Test
    void spellWithoutApprovedCostRuleSpendsNoCombatAction() {
        knownSpell(3);
        when(slotService.expendInternal(caster.getId(), 3))
                .thenReturn(SpellSlotsResponse.builder().build());

        service.cast(caster, spellId,
                SpellCastRequest.builder().combatId(UUID.randomUUID()).build(), null);

        verify(economyService, never()).spend(any(), any(), anyString());
    }

    @Test
    void effectsLandOnTheExplicitTarget() {
        knownSpell(3);
        when(slotService.expendInternal(caster.getId(), 3))
                .thenReturn(SpellSlotsResponse.builder().build());
        PlayerCharacter ally = PlayerCharacter.builder().id(UUID.randomUUID()).build();
        when(effectService.applyForSpellCast(caster, ally, spellId)).thenReturn(1);

        SpellCastResult result = service.cast(caster, spellId,
                SpellCastRequest.builder().targetCharacterId(ally.getId()).build(), ally);

        assertThat(result.getEffectsApplied()).isEqualTo(1);
        verify(effectService).applyForSpellCast(caster, ally, spellId);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Spell loaded;

    private void knownSpell(int level) {
        loaded = spell(level);
        when(flags.spellsActive()).thenReturn(true);
        when(spellRepository.findById(spellId)).thenReturn(Optional.of(loaded));
        when(knownSpellRepository.existsByCharacterIdAndSpellId(caster.getId(), spellId)).thenReturn(true);
        lenient().when(resolver.approvedEnabledRules(eq(FeatureRuleOwnerType.SPELL), anyList()))
                .thenReturn(List.of());
    }

    private Spell spellAsLoaded() {
        return loaded;
    }
}
