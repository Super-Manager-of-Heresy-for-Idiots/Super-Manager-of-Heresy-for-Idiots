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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
    @Mock private EntityManager entityManager;

    @InjectMocks private FeatureResourceService service;

    private final UUID charId = UUID.randomUUID();
    private final UUID resId = UUID.randomUUID();
    private final UUID defId = UUID.randomUUID();

    @BeforeEach
    void injectEntityManager() {
        // @PersistenceContext-поле не идёт через конструктор, а @InjectMocks выбирает конструкторную
        // инъекцию — поэтому entityManager проставляем вручную.
        ReflectionTestUtils.setField(service, "entityManager", entityManager);
    }

    private CharacterFeatureResource resource(int current, Integer max) {
        return CharacterFeatureResource.builder()
                .id(resId).characterId(charId).resourceDefinitionId(defId)
                .currentValue(current).maxValueSnapshot(max).build();
    }

    private FeatureResourceDefinition def(boolean allowNegative) {
        return FeatureResourceDefinition.builder().id(defId).allowNegative(allowNegative).build();
    }

    @Test
    void spendRejectsWhenAtomicUpdateChangesNoRows() {
        when(resourceRepository.findById(resId)).thenReturn(Optional.of(resource(2, 3)));
        when(definitionRepository.findById(defId)).thenReturn(Optional.of(def(false)));
        when(resourceRepository.spendAtomically(resId, charId, 3, false)).thenReturn(0);

        assertThatThrownBy(() -> service.spend(charId, resId, 3))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Недостаточно ресурса для использования");
        verify(entityManager, never()).refresh(any());
    }

    @Test
    void spendDecrementsWhenSufficient() {
        CharacterFeatureResource res = resource(2, 3);
        when(resourceRepository.findById(resId)).thenReturn(Optional.of(res));
        when(definitionRepository.findById(defId)).thenReturn(Optional.of(def(false)));
        when(resourceRepository.spendAtomically(resId, charId, 1, false)).thenReturn(1);
        // Симулируем перечитывание строки из БД после атомарного списания.
        doAnswer(inv -> { res.setCurrentValue(1); return null; }).when(entityManager).refresh(res);

        CharacterFeatureResource result = service.spend(charId, resId, 1);

        assertThat(result.getCurrentValue()).isEqualTo(1);
        verify(resourceRepository).spendAtomically(resId, charId, 1, false);
    }

    @Test
    void spendAllowsNegativeWhenConfigured() {
        CharacterFeatureResource res = resource(0, 3);
        when(resourceRepository.findById(resId)).thenReturn(Optional.of(res));
        when(definitionRepository.findById(defId)).thenReturn(Optional.of(def(true)));
        when(resourceRepository.spendAtomically(resId, charId, 2, true)).thenReturn(1);
        doAnswer(inv -> { res.setCurrentValue(-2); return null; }).when(entityManager).refresh(res);

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
