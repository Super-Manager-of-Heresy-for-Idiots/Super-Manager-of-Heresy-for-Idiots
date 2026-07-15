package com.dnd.app.service;

import com.dnd.app.domain.featurerule.FeatureItemBinding;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.dto.featurerule.FeatureItemBindingEditRequest;
import com.dnd.app.dto.featurerule.FeatureItemBindingResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.FeatureItemBindingRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тест FeatureItemBindingAdminServiceTest (ITEM_ABIL Фаза 4): чтение/правка binding item-правила + инварианты
 * (только у item-owner; consume ⨯ equipped запрещён).
 */
@ExtendWith(MockitoExtension.class)
class FeatureItemBindingAdminServiceTest {

    @Mock private FeatureRuleRepository ruleRepository;
    @Mock private FeatureItemBindingRepository bindingRepository;

    @InjectMocks private FeatureItemBindingAdminService service;

    private FeatureRule itemRule(UUID id) {
        FeatureRule r = FeatureRule.builder().ownerType(FeatureRuleOwnerType.ITEM_MAGIC.getCode()).build();
        r.setId(id);
        return r;
    }

    @Test
    @DisplayName("upsert: валидный binding сохраняется")
    void upsert_valid_saves() {
        UUID ruleId = UUID.randomUUID();
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(itemRule(ruleId)));
        when(bindingRepository.findByFeatureRuleId(ruleId)).thenReturn(Optional.empty());
        when(bindingRepository.save(any(FeatureItemBinding.class))).thenAnswer(inv -> inv.getArgument(0));

        FeatureItemBindingEditRequest req = new FeatureItemBindingEditRequest();
        req.setRequiresAttunement(true);
        req.setConsumeOnUse(false);
        req.setRequiresEquipped(true);

        FeatureItemBindingResponse resp = service.upsert(ruleId, req);

        assertTrue(resp.isRequiresAttunement());
        assertTrue(resp.isRequiresEquipped());
        assertEquals(1, resp.getConsumeQuantity());
        verify(bindingRepository).save(any(FeatureItemBinding.class));
    }

    @Test
    @DisplayName("upsert: consume_on_use ⨯ requires_equipped → 400")
    void upsert_consumeAndEquipped_rejected() {
        UUID ruleId = UUID.randomUUID();
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(itemRule(ruleId)));

        FeatureItemBindingEditRequest req = new FeatureItemBindingEditRequest();
        req.setConsumeOnUse(true);
        req.setRequiresEquipped(true);

        assertThrows(BadRequestException.class, () -> service.upsert(ruleId, req));
        verify(bindingRepository, never()).save(any());
    }

    @Test
    @DisplayName("upsert: не-item правило → 400")
    void upsert_nonItemRule_rejected() {
        UUID ruleId = UUID.randomUUID();
        FeatureRule classRule = FeatureRule.builder()
                .ownerType(FeatureRuleOwnerType.CLASS_FEATURE.getCode()).build();
        classRule.setId(ruleId);
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(classRule));

        assertThrows(BadRequestException.class,
                () -> service.upsert(ruleId, new FeatureItemBindingEditRequest()));
    }

    @Test
    @DisplayName("get: без binding возвращает заготовку с consumeQuantity=1")
    void get_noBinding_returnsDefault() {
        UUID ruleId = UUID.randomUUID();
        when(ruleRepository.findById(ruleId)).thenReturn(Optional.of(itemRule(ruleId)));
        when(bindingRepository.findByFeatureRuleId(ruleId)).thenReturn(Optional.empty());

        FeatureItemBindingResponse resp = service.get(ruleId);

        assertEquals(ruleId, resp.getFeatureRuleId());
        assertEquals(1, resp.getConsumeQuantity());
        assertFalse(resp.isConsumeOnUse());
    }
}
