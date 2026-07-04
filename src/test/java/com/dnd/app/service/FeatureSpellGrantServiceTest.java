package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureSpellGrant;
import com.dnd.app.dto.featurerule.FeatureSpellGrantResponse;
import com.dnd.app.repository.CharacterFeatureResourceRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
import com.dnd.app.repository.FeatureSpellFilterRepository;
import com.dnd.app.repository.FeatureSpellGrantRepository;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import com.dnd.app.service.formula.FormulaContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureSpellGrantServiceTest {

    @Mock private FeatureRulesProperties flags;
    @Mock private CharacterFeatureResolver resolver;
    @Mock private FeatureSpellGrantRepository grantRepository;
    @Mock private FeatureSpellFilterRepository filterRepository;
    @Mock private FeatureFormulaRepository formulaRepository;
    @Mock private FeatureFormulaService formulaService;
    @Mock private CharacterFormulaContextFactory contextFactory;
    @Mock private CharacterFeatureResourceRepository resourceRepository;
    @Mock private FeatureResourceDefinitionRepository resourceDefinitionRepository;
    @Mock private FeatureResourceService featureResourceService;

    @InjectMocks private FeatureSpellGrantService service;

    @Test
    void listIsEmptyWhenSpellsFlagInactive() {
        when(flags.spellsActive()).thenReturn(false);

        List<FeatureSpellGrantResponse> result =
                service.listGrantedSpells(PlayerCharacter.builder().id(UUID.randomUUID()).build());

        assertThat(result).isEmpty();
        verifyNoInteractions(resolver, grantRepository, filterRepository);
    }

    @Test
    void listsFeatureGrantedSpellsWithFlags() {
        when(flags.spellsActive()).thenReturn(true);

        UUID featureId = UUID.randomUUID();
        ClassFeature feature = ClassFeature.builder().id(featureId).title("Divine Order").build();
        when(resolver.knownBaseClassFeatures(any())).thenReturn(List.of(feature));

        FeatureRule rule = FeatureRule.builder().id(UUID.randomUUID())
                .ownerType("CLASS_FEATURE").ownerId(featureId).build();
        when(resolver.approvedEnabledRules(anyCollection())).thenReturn(List.of(rule));

        UUID spellId = UUID.randomUUID();
        FeatureSpellGrant grant = FeatureSpellGrant.builder()
                .id(UUID.randomUUID()).featureRuleId(rule.getId())
                .spellId(spellId).alwaysPrepared(true).countsAgainstKnown(false).build();
        when(grantRepository.findByFeatureRuleIdIn(anyCollection())).thenReturn(List.of(grant));
        when(contextFactory.build(any())).thenReturn(org.mockito.Mockito.mock(FormulaContext.class));

        List<FeatureSpellGrantResponse> result =
                service.listGrantedSpells(PlayerCharacter.builder().id(UUID.randomUUID()).build());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSpellId()).isEqualTo(spellId);
        assertThat(result.get(0).isAlwaysPrepared()).isTrue();
        assertThat(result.get(0).getFeatureName()).isEqualTo("Divine Order");
    }
}
