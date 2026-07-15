package com.dnd.app.service;

import com.dnd.app.domain.featurerule.FeatureItemBinding;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.dto.featurerule.FeatureItemBindingEditRequest;
import com.dnd.app.dto.featurerule.FeatureItemBindingResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.FeatureItemBindingRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Админ-CRUD привязки item-правила к предмету (ITEM_ABIL Фаза 4, Rule Workbench). Позволяет админу читать и править
 * {@code feature_item_binding} (гейтинг умения: экипировка/аттюнмент/расход/слот). Инвариант валидатора:
 * binding допустим только у item-owner правила; {@code consume_on_use} несовместим с {@code requires_equipped}.
 */
@Service
@RequiredArgsConstructor
public class FeatureItemBindingAdminService {

    private final FeatureRuleRepository ruleRepository;
    private final FeatureItemBindingRepository bindingRepository;

    /**
     * Возвращает привязку правила (или пустую заготовку, если ещё не создана).
     * @param ruleId идентификатор item-правила
     * @return настройки привязки
     */
    @Transactional(readOnly = true)
    public FeatureItemBindingResponse get(UUID ruleId) {
        requireItemRule(ruleId);
        return bindingRepository.findByFeatureRuleId(ruleId)
                .map(this::toResponse)
                .orElseGet(() -> FeatureItemBindingResponse.builder()
                        .featureRuleId(ruleId)
                        .consumeQuantity(1)
                        .build());
    }

    /**
     * Создаёт/обновляет привязку item-правила.
     * @param ruleId идентификатор item-правила
     * @param request новые настройки гейтинга
     * @return обновлённая привязка
     */
    @Transactional
    public FeatureItemBindingResponse upsert(UUID ruleId, FeatureItemBindingEditRequest request) {
        requireItemRule(ruleId);
        boolean consume = Boolean.TRUE.equals(request.getConsumeOnUse());
        boolean equipped = Boolean.TRUE.equals(request.getRequiresEquipped());
        if (consume && equipped) {
            throw new BadRequestException("consume_on_use несовместим с requires_equipped");
        }
        FeatureItemBinding binding = bindingRepository.findByFeatureRuleId(ruleId)
                .orElseGet(() -> FeatureItemBinding.builder().featureRuleId(ruleId).build());
        binding.setRequiresEquipped(equipped);
        binding.setRequiresAttunement(Boolean.TRUE.equals(request.getRequiresAttunement()));
        binding.setConsumeOnUse(consume);
        binding.setConsumeQuantity(request.getConsumeQuantity() != null && request.getConsumeQuantity() > 0
                ? request.getConsumeQuantity() : 1);
        binding.setAllowedSlotCode(request.getAllowedSlotCode());
        return toResponse(bindingRepository.save(binding));
    }

    private FeatureRule requireItemRule(UUID ruleId) {
        FeatureRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Правило не найдено: " + ruleId));
        boolean itemOwner = FeatureRuleOwnerType.fromCode(rule.getOwnerType())
                .map(t -> t.isItemOwner()).orElse(false);
        if (!itemOwner) {
            throw new BadRequestException("Привязка допустима только у правил предметов (ITEM_*)");
        }
        return rule;
    }

    private FeatureItemBindingResponse toResponse(FeatureItemBinding b) {
        return FeatureItemBindingResponse.builder()
                .featureRuleId(b.getFeatureRuleId())
                .requiresEquipped(b.isRequiresEquipped())
                .requiresAttunement(b.isRequiresAttunement())
                .consumeOnUse(b.isConsumeOnUse())
                .consumeQuantity(b.getConsumeQuantity())
                .allowedSlotCode(b.getAllowedSlotCode())
                .build();
    }
}
