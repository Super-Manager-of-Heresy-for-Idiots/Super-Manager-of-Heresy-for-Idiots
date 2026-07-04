package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.featurerule.FeatureActiveEffect;
import com.dnd.app.domain.featurerule.FeatureEffectDefinition;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.repository.DurationUnitRepository;
import com.dnd.app.repository.FeatureActiveEffectRepository;
import com.dnd.app.repository.FeatureEffectDefinitionRepository;
import com.dnd.app.repository.FeatureEffectEndConditionRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import com.dnd.app.service.formula.FormulaContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureEffectServiceTest {

    @Mock private FeatureRulesProperties flags;
    @Mock private CharacterFeatureResolver resolver;
    @Mock private FeatureEffectDefinitionRepository definitionRepository;
    @Mock private FeatureEffectEndConditionRepository endConditionRepository;
    @Mock private FeatureActiveEffectRepository activeRepository;
    @Mock private FeatureFormulaRepository formulaRepository;
    @Mock private FeatureFormulaService formulaService;
    @Mock private DurationUnitRepository durationUnitRepository;
    @Mock private CharacterFormulaContextFactory contextFactory;

    @InjectMocks private FeatureEffectService service;

    private final UUID featureId = UUID.randomUUID();

    @Test
    void noOpWhenEffectsFlagInactive() {
        when(flags.effectsActive()).thenReturn(false);

        int created = service.applyForFeatureUse(PlayerCharacter.builder().id(UUID.randomUUID()).build(), featureId);

        assertThat(created).isZero();
        verifyNoInteractions(resolver, definitionRepository, activeRepository);
    }

    @Test
    void createsActiveEffectWhenEnabled() {
        when(flags.effectsActive()).thenReturn(true);

        FeatureRule rule = FeatureRule.builder().id(UUID.randomUUID()).build();
        when(resolver.approvedEnabledRules(anyList())).thenReturn(List.of(rule));

        FeatureEffectDefinition def = FeatureEffectDefinition.builder()
                .id(UUID.randomUUID()).featureRuleId(rule.getId())
                .effectKey("rage").stackingPolicy("stack").build();
        when(definitionRepository.findByFeatureRuleIdIn(anyList())).thenReturn(List.of(def));
        when(endConditionRepository.findByEffectDefinitionId(def.getId())).thenReturn(List.of());
        when(contextFactory.build(any())).thenReturn(org.mockito.Mockito.mock(FormulaContext.class));

        UUID charId = UUID.randomUUID();
        int created = service.applyForFeatureUse(PlayerCharacter.builder().id(charId).build(), featureId);

        assertThat(created).isEqualTo(1);
        ArgumentCaptor<FeatureActiveEffect> captor = ArgumentCaptor.forClass(FeatureActiveEffect.class);
        verify(activeRepository).save(captor.capture());
        assertThat(captor.getValue().getEffectDefinitionId()).isEqualTo(def.getId());
        assertThat(captor.getValue().getStatus()).isEqualTo("active");
        assertThat(captor.getValue().getCharacterId()).isEqualTo(charId);
    }
}
