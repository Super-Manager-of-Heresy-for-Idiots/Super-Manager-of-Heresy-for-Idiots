package com.dnd.app.service;

import com.dnd.app.domain.featurerule.ActiveEffectStatus;
import com.dnd.app.domain.featurerule.FeatureActiveEffect;
import com.dnd.app.domain.featurerule.FeatureEffectDefinition;
import com.dnd.app.domain.featurerule.FeatureEffectModifier;
import com.dnd.app.dto.featurerule.ActiveEffectResponse;
import com.dnd.app.repository.FeatureActiveEffectRepository;
import com.dnd.app.repository.FeatureEffectDefinitionRepository;
import com.dnd.app.repository.FeatureEffectModifierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Класс ActiveEffectQueryService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class ActiveEffectQueryService {

    private static final String ACTIVE = ActiveEffectStatus.ACTIVE.getCode();

    private final FeatureActiveEffectRepository activeRepository;
    private final FeatureEffectDefinitionRepository definitionRepository;
    private final FeatureEffectModifierRepository modifierRepository;

    /**
     * Возвращает список для операции "list active" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ActiveEffectResponse> listActive(UUID characterId) {
        List<FeatureActiveEffect> effects = activeRepository.findByCharacterIdAndStatus(characterId, ACTIVE);
        if (effects.isEmpty()) {
            return List.of();
        }
        List<UUID> defIds = effects.stream().map(FeatureActiveEffect::getEffectDefinitionId).distinct().toList();
        Map<UUID, FeatureEffectDefinition> defs = definitionRepository.findAllById(defIds).stream()
                .collect(Collectors.toMap(FeatureEffectDefinition::getId, d -> d));
        Map<UUID, List<FeatureEffectModifier>> modsByDef = modifierRepository.findByEffectDefinitionIdIn(defIds)
                .stream().collect(Collectors.groupingBy(FeatureEffectModifier::getEffectDefinitionId));

        return effects.stream().map(effect -> {
            FeatureEffectDefinition def = defs.get(effect.getEffectDefinitionId());
            List<ActiveEffectResponse.Modifier> modifiers = modsByDef
                    .getOrDefault(effect.getEffectDefinitionId(), List.of()).stream()
                    .map(m -> ActiveEffectResponse.Modifier.builder()
                            .id(m.getId())
                            .modifierType(m.getModifierType())
                            .valueFormulaId(m.getValueFormulaId())
                            .abilityId(m.getAbilityId())
                            .skillId(m.getSkillId())
                            .damageTypeId(m.getDamageTypeId())
                            .conditionId(m.getConditionId())
                            .build())
                    .toList();
            return ActiveEffectResponse.builder()
                    .id(effect.getId())
                    .effectDefinitionId(effect.getEffectDefinitionId())
                    .effectKey(def != null ? def.getEffectKey() : null)
                    .displayName(def != null ? def.getDisplayName() : null)
                    .sourceFeatureId(effect.getSourceFeatureId())
                    .sourceCharacterId(effect.getSourceCharacterId())
                    .startedAt(effect.getStartedAt())
                    .expiresAt(effect.getExpiresAt())
                    .remainingRounds(effect.getRemainingRounds())
                    .status(effect.getStatus())
                    .concentrationRequired(def != null && def.isConcentrationRequired())
                    .stackingPolicy(def != null ? def.getStackingPolicy() : null)
                    .activeEffectGroup(def != null ? def.getActiveEffectGroup() : null)
                    .modifiers(modifiers)
                    .build();
        }).toList();
    }
}
