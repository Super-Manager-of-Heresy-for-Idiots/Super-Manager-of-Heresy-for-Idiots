package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.featurerule.CharacterFeatureResource;
import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.CharacterFeatureResourceRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureResourceServiceTest {

    @Mock private FeatureRulesProperties flags;
    @Mock private FeatureResourceDefinitionRepository definitionRepository;
    @Mock private CharacterFeatureResourceRepository resourceRepository;
    @Mock private FeatureFormulaRepository formulaRepository;
    @Mock private FeatureFormulaService formulaService;
    @Mock private CharacterFormulaContextFactory contextFactory;

    @InjectMocks private FeatureResourceService service;

    private final UUID charId = UUID.randomUUID();
    private final UUID resId = UUID.randomUUID();
    private final UUID defId = UUID.randomUUID();

    private CharacterFeatureResource resource(int current, Integer max) {
        return CharacterFeatureResource.builder()
                .id(resId).characterId(charId).resourceDefinitionId(defId)
                .currentValue(current).maxValueSnapshot(max).build();
    }

    @Test
    void spendRejectsGoingBelowZeroWhenNotAllowed() {
        when(resourceRepository.findById(resId)).thenReturn(Optional.of(resource(2, 3)));
        when(definitionRepository.findById(defId))
                .thenReturn(Optional.of(FeatureResourceDefinition.builder().id(defId).allowNegative(false).build()));

        assertThatThrownBy(() -> service.spend(charId, resId, 3))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void spendDecrementsWhenSufficient() {
        when(resourceRepository.findById(resId)).thenReturn(Optional.of(resource(2, 3)));
        when(definitionRepository.findById(defId))
                .thenReturn(Optional.of(FeatureResourceDefinition.builder().id(defId).allowNegative(false).build()));
        when(resourceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CharacterFeatureResource result = service.spend(charId, resId, 1);
        assertThat(result.getCurrentValue()).isEqualTo(1);
    }

    @Test
    void spendAllowsNegativeWhenConfigured() {
        when(resourceRepository.findById(resId)).thenReturn(Optional.of(resource(0, 3)));
        when(definitionRepository.findById(defId))
                .thenReturn(Optional.of(FeatureResourceDefinition.builder().id(defId).allowNegative(true).build()));
        when(resourceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CharacterFeatureResource result = service.spend(charId, resId, 2);
        assertThat(result.getCurrentValue()).isEqualTo(-2);
    }

    @Test
    void setValueClampsToMax() {
        when(resourceRepository.findById(resId)).thenReturn(Optional.of(resource(1, 3)));
        when(resourceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CharacterFeatureResource result = service.setValue(charId, resId, 9);
        assertThat(result.getCurrentValue()).isEqualTo(3);
    }

    @Test
    void ensureIsNoOpWhenResourcesFlagInactive() {
        when(flags.resourcesActive()).thenReturn(false);

        service.ensureResourcesForRules(PlayerCharacter.builder().id(charId).build(), List.of(UUID.randomUUID()));

        verifyNoInteractions(definitionRepository, resourceRepository, formulaRepository,
                formulaService, contextFactory);
    }
}
