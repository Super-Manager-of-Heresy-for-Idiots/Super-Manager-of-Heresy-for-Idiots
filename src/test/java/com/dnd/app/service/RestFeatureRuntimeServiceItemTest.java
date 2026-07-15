package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.ItemInstance;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import com.dnd.app.domain.featurerule.ItemInstanceFeatureResource;
import com.dnd.app.domain.featurerule.RestType;
import com.dnd.app.dto.featurerule.RestResourcePreview;
import com.dnd.app.repository.CharacterFeatureResourceRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
import com.dnd.app.repository.ItemInstanceFeatureResourceRepository;
import com.dnd.app.repository.ItemInstanceRepository;
import com.dnd.app.repository.RestTypeRepository;
import com.dnd.app.service.formula.CharacterFormulaContextFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Тест восстановления зарядов предметов на отдыхе (ITEM_ABIL Фаза 3 §4.4): {@code complete()} с включённым флагом
 * items возвращает item-ресурс к максимуму на подходящем типе отдыха; при выключенном флаге предметы не трогаются.
 */
@ExtendWith(MockitoExtension.class)
class RestFeatureRuntimeServiceItemTest {

    @Mock private FeatureRulesProperties flags;
    @Mock private CharacterFeatureResourceRepository resourceRepository;
    @Mock private FeatureResourceDefinitionRepository definitionRepository;
    @Mock private RestTypeRepository restTypeRepository;
    @Mock private FeatureFormulaRepository formulaRepository;
    @Mock private FeatureFormulaService formulaService;
    @Mock private CharacterFormulaContextFactory contextFactory;
    @Mock private EffectExpirationService effectExpirationService;
    @Mock private ItemInstanceFeatureResourceRepository itemResourceRepository;
    @Mock private ItemInstanceRepository itemInstanceRepository;

    @InjectMocks private RestFeatureRuntimeService service;

    @Test
    @DisplayName("complete(long_rest): восстанавливает заряд предмета до максимума (флаг items включён)")
    void complete_restoresItemChargesOnMatchingRest() {
        UUID charId = UUID.randomUUID();
        UUID instanceId = UUID.randomUUID();
        UUID defId = UUID.randomUUID();
        UUID restTypeId = UUID.randomUUID();

        PlayerCharacter character = mock(PlayerCharacter.class);
        when(character.getId()).thenReturn(charId);

        RestType longRest = mock(RestType.class);
        when(longRest.getId()).thenReturn(restTypeId);

        ItemInstance instance = mock(ItemInstance.class);
        when(instance.getId()).thenReturn(instanceId);

        ItemInstanceFeatureResource res = new ItemInstanceFeatureResource();
        res.setId(UUID.randomUUID());
        res.setItemInstanceId(instanceId);
        res.setResourceDefinitionId(defId);
        res.setCurrentValue(0);
        res.setMaxValueSnapshot(3);

        FeatureResourceDefinition def = new FeatureResourceDefinition();
        def.setId(defId);
        def.setResetRestTypeId(restTypeId);   // сбрасывается на этом отдыхе
        def.setResourceKey("wand_charges");
        def.setDisplayName("Заряды жезла");

        when(flags.resourcesActive()).thenReturn(false); // пропускаем character-ветку
        when(flags.itemsActive()).thenReturn(true);
        when(restTypeRepository.findByCode("long_rest")).thenReturn(java.util.Optional.of(longRest));
        when(itemInstanceRepository.findByOwnerCharacterId(charId)).thenReturn(List.of(instance));
        when(itemResourceRepository.findByItemInstanceIdIn(List.of(instanceId))).thenReturn(List.of(res));
        when(definitionRepository.findAllById(List.of(defId))).thenReturn(List.of(def));

        List<RestResourcePreview> restored = service.complete(character, "long_rest");

        assertEquals(1, restored.size());
        assertEquals(3, restored.get(0).getWillBeValue());
        assertEquals(3, res.getCurrentValue()); // применено
        verify(itemResourceRepository).save(res);
        verify(effectExpirationService).endOnRest(eq(character), eq("long_rest"));
    }

    @Test
    @DisplayName("complete: флаг items выключен — заряды предметов не трогаются")
    void complete_itemsFlagOff_skipsItems() {
        PlayerCharacter character = mock(PlayerCharacter.class);
        when(flags.resourcesActive()).thenReturn(false);
        when(flags.itemsActive()).thenReturn(false);

        List<RestResourcePreview> restored = service.complete(character, "long_rest");

        assertTrue(restored.isEmpty());
        verify(itemInstanceRepository, never()).findByOwnerCharacterId(any());
        verify(itemResourceRepository, never()).save(any());
    }
}
