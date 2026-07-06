package com.dnd.app.service;

import com.dnd.app.domain.BuffDebuff;
import com.dnd.app.domain.CharacterActiveEffect;
import com.dnd.app.domain.DamageType;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.content.SpeciesTraitEffect;
import com.dnd.app.domain.featurerule.FeatureActiveEffect;
import com.dnd.app.domain.featurerule.FeatureEffectDefinition;
import com.dnd.app.domain.featurerule.FeatureEffectModifier;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.dto.combat.ModifierTarget;
import com.dnd.app.repository.CharacterActiveEffectRepository;
import com.dnd.app.repository.FeatureActiveEffectRepository;
import com.dnd.app.repository.FeatureEffectDefinitionRepository;
import com.dnd.app.repository.FeatureEffectModifierRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.SpeciesTraitEffectRepository;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import com.dnd.app.service.formula.FormulaContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModifierAggregatorTest {

    @Mock private CharacterActiveEffectRepository characterActiveEffectRepository;
    @Mock private FeatureActiveEffectRepository featureActiveEffectRepository;
    @Mock private FeatureEffectModifierRepository featureEffectModifierRepository;
    @Mock private FeatureEffectDefinitionRepository featureEffectDefinitionRepository;
    @Mock private FeatureFormulaRepository formulaRepository;
    @Mock private FeatureFormulaService formulaService;
    @Mock private CharacterFormulaContextFactory contextFactory;
    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private SpeciesTraitEffectRepository speciesTraitEffectRepository;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private ModifierAggregator aggregator;

    private final UUID characterId = UUID.randomUUID();
    private final UUID strStatId = UUID.randomUUID();

    private StatType str() {
        return StatType.builder().id(strStatId).slug("str").build();
    }

    private CharacterActiveEffect buff(String name, boolean isBuff, int value) {
        BuffDebuff bd = BuffDebuff.builder().id(UUID.randomUUID()).name(name).effectType("STAT_MODIFIER")
                .isBuff(isBuff).modifierValue(value).targetStat(str()).build();
        return CharacterActiveEffect.builder().id(UUID.randomUUID()).buffDebuff(bd).build();
    }

    @Test
    void legacyStatModifiersFromDifferentSourcesSum() {
        when(characterActiveEffectRepository.findByCharacterId(characterId))
                .thenReturn(List.of(buff("Bull's Strength", true, 2), buff("Weakness", false, 1)));
        when(featureActiveEffectRepository.findByCharacterIdAndStatus(characterId, "active")).thenReturn(List.of());

        int total = aggregator.totalFor(characterId, ModifierTarget.statCheck(strStatId, "str"));

        assertThat(total).isEqualTo(1); // +2 buff and -1 debuff, distinct sources → sum
    }

    @Test
    void featureAcModifierIsEvaluatedFromItsFormula() {
        UUID defId = UUID.randomUUID();
        UUID formulaId = UUID.randomUUID();
        FeatureActiveEffect effect = FeatureActiveEffect.builder()
                .id(UUID.randomUUID()).characterId(characterId).effectDefinitionId(defId).status("active").build();
        FeatureEffectModifier mod = FeatureEffectModifier.builder()
                .id(UUID.randomUUID()).effectDefinitionId(defId).modifierType("ac_bonus").valueFormulaId(formulaId).build();
        FeatureEffectDefinition def = FeatureEffectDefinition.builder()
                .id(defId).effectKey("shield_of_faith").stackingPolicy("highest_only").build();
        FeatureFormula formula = FeatureFormula.builder().id(formulaId).expression("2").build();

        // AC is not stat-scoped → legacy source is never consulted (no stub for it).
        when(featureActiveEffectRepository.findByCharacterIdAndStatus(characterId, "active")).thenReturn(List.of(effect));
        when(featureEffectModifierRepository.findByEffectDefinitionIdIn(any())).thenReturn(List.of(mod));
        when(featureEffectDefinitionRepository.findAllById(any())).thenReturn(List.of(def));
        when(characterRepository.findById(characterId))
                .thenReturn(Optional.of(PlayerCharacter.builder().id(characterId).build()));
        when(contextFactory.build(any())).thenReturn(mock(FormulaContext.class));
        when(formulaRepository.findById(formulaId)).thenReturn(Optional.of(formula));
        when(formulaService.evaluateInteger(eq(formula), any())).thenReturn(2);

        assertThat(aggregator.totalFor(characterId, ModifierTarget.ac())).isEqualTo(2);
    }

    @Test
    void resistanceHalvesAndUnrelatedTypeIsUnaffected() {
        UUID defId = UUID.randomUUID();
        UUID fireId = UUID.randomUUID();
        FeatureActiveEffect effect = FeatureActiveEffect.builder()
                .id(UUID.randomUUID()).characterId(characterId).effectDefinitionId(defId).status("active").build();
        FeatureEffectModifier resist = FeatureEffectModifier.builder()
                .id(UUID.randomUUID()).effectDefinitionId(defId).modifierType("damage_resistance").damageTypeId(fireId).build();
        when(featureActiveEffectRepository.findByCharacterIdAndStatus(characterId, "active")).thenReturn(List.of(effect));
        when(featureEffectModifierRepository.findByEffectDefinitionIdIn(any())).thenReturn(List.of(resist));

        assertThat(aggregator.damageMultiplier(characterId, fireId)).isEqualTo(0.5);
        assertThat(aggregator.damageMultiplier(characterId, UUID.randomUUID())).isEqualTo(1.0);
    }

    @Test
    void bridgedSpellBuffDoesNotStackWithTheSameLegacyBuff() {
        // The same "+2 STR" buff is active twice: applied by an item through the legacy system AND by a
        // cast spell through the backfilled feature effect ("buff:<name>" effect key). The bridged stack
        // key must collapse them to the max (+2), not sum to +4.
        when(characterActiveEffectRepository.findByCharacterId(characterId))
                .thenReturn(List.of(buff("Bull's Strength", true, 2)));

        UUID defId = UUID.randomUUID();
        UUID formulaId = UUID.randomUUID();
        FeatureActiveEffect effect = FeatureActiveEffect.builder()
                .id(UUID.randomUUID()).characterId(characterId).effectDefinitionId(defId).status("active").build();
        FeatureEffectModifier mod = FeatureEffectModifier.builder()
                .id(UUID.randomUUID()).effectDefinitionId(defId).modifierType("stat_bonus")
                .abilityId(strStatId).valueFormulaId(formulaId).build();
        FeatureEffectDefinition def = FeatureEffectDefinition.builder()
                .id(defId).effectKey("buff:Bull's Strength").stackingPolicy("replace_same_feature").build();
        when(featureActiveEffectRepository.findByCharacterIdAndStatus(characterId, "active")).thenReturn(List.of(effect));
        when(featureEffectModifierRepository.findByEffectDefinitionIdIn(any())).thenReturn(List.of(mod));
        when(featureEffectDefinitionRepository.findAllById(any())).thenReturn(List.of(def));
        when(characterRepository.findById(characterId))
                .thenReturn(Optional.of(PlayerCharacter.builder().id(characterId).build()));
        when(contextFactory.build(any())).thenReturn(mock(FormulaContext.class));
        when(formulaRepository.findById(formulaId))
                .thenReturn(Optional.of(FeatureFormula.builder().id(formulaId).expression("2").build()));
        when(formulaService.evaluateInteger(any(FeatureFormula.class), any())).thenReturn(2);

        int total = aggregator.totalFor(characterId, ModifierTarget.statCheck(strStatId, "str"));

        assertThat(total).isEqualTo(2);
    }

    @Test
    void featureTotalReadsFeatureSourceOnly() {
        when(featureActiveEffectRepository.findByCharacterIdAndStatus(characterId, "active")).thenReturn(List.of());

        int total = aggregator.featureTotal(characterId, ModifierTarget.statCheck(strStatId, "str"));

        assertThat(total).isZero();
        verifyNoInteractions(characterActiveEffectRepository); // must not consult the legacy source
    }

    @Test
    void racialResistanceHalvesMatchingDamageType() {
        UUID speciesId = UUID.randomUUID();
        UUID fireId = UUID.randomUUID();
        // No feature effects — the resistance comes purely from the species (Source C).
        when(featureActiveEffectRepository.findByCharacterIdAndStatus(characterId, "active")).thenReturn(List.of());
        when(characterRepository.findById(characterId)).thenReturn(Optional.of(
                PlayerCharacter.builder().id(characterId)
                        .raceSnapshotJson("{\"raceId\":\"" + speciesId + "\"}").build()));
        when(speciesTraitEffectRepository.findBySpeciesIdAndEffectType(speciesId, "resistance"))
                .thenReturn(List.of(SpeciesTraitEffect.builder()
                        .effectType("resistance").damageType(DamageType.builder().id(fireId).build()).build()));

        assertThat(aggregator.damageMultiplier(characterId, fireId)).isEqualTo(0.5);
    }
}
