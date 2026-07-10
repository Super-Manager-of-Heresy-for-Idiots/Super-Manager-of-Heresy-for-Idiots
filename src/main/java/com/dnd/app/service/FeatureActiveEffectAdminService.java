package com.dnd.app.service;

import com.dnd.app.domain.featurerule.EffectStackingPolicy;
import com.dnd.app.domain.featurerule.FeatureEffectDefinition;
import com.dnd.app.domain.featurerule.FeatureEffectEndCondition;
import com.dnd.app.domain.featurerule.FeatureEffectModifier;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FormulaResultType;
import com.dnd.app.dto.featurerule.ActiveEffectAdminResponse;
import com.dnd.app.dto.featurerule.ActiveEffectEditRequest;
import com.dnd.app.dto.featurerule.EffectMetadataResponse;
import com.dnd.app.dto.featurerule.RuleRefOption;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.DurationUnitRepository;
import com.dnd.app.repository.FeatureEffectDefinitionRepository;
import com.dnd.app.repository.FeatureEffectEndConditionRepository;
import com.dnd.app.repository.FeatureEffectModifierRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import com.dnd.app.repository.RestTypeRepository;
import com.dnd.app.repository.TargetTypeRepository;
import com.dnd.app.repository.TriggerEventTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Класс FeatureActiveEffectAdminService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class FeatureActiveEffectAdminService {

    private final FeatureRuleRepository ruleRepository;
    private final FeatureEffectDefinitionRepository definitionRepository;
    private final FeatureEffectModifierRepository modifierRepository;
    private final FeatureEffectEndConditionRepository endConditionRepository;
    private final DurationUnitRepository durationUnitRepository;
    private final RestTypeRepository restTypeRepository;
    private final TargetTypeRepository targetTypeRepository;
    private final TriggerEventTypeRepository triggerEventTypeRepository;
    private final FeatureFormulaAdminHelper formulaHelper;

    /**
     * Выполняет операции "metadata" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public EffectMetadataResponse metadata() {
        return EffectMetadataResponse.builder()
                .durationUnits(durationUnitRepository.findAll().stream()
                        .sorted(Comparator.comparing(du -> nz(du.getSortOrder())))
                        .map(du -> RuleRefOption.builder().id(du.getId()).code(du.getCode()).label(du.getDisplayName()).build())
                        .toList())
                .restTypes(restTypeRepository.findAll().stream()
                        .sorted(Comparator.comparing(rt -> nz(rt.getSortOrder())))
                        .map(rt -> RuleRefOption.builder().id(rt.getId()).code(rt.getCode()).label(rt.getDisplayName()).build())
                        .toList())
                .targetTypes(targetTypeRepository.findAll().stream()
                        .sorted(Comparator.comparing(tt -> nz(tt.getSortOrder())))
                        .map(tt -> RuleRefOption.builder().id(tt.getId()).code(tt.getCode()).label(tt.getDisplayName()).build())
                        .toList())
                .triggerEventTypes(triggerEventTypeRepository.findAll().stream()
                        .sorted(Comparator.comparing(te -> nz(te.getSortOrder())))
                        .map(te -> RuleRefOption.builder().id(te.getId()).code(te.getCode()).label(te.getDisplayName()).build())
                        .toList())
                .stackingPolicies(java.util.Arrays.stream(EffectStackingPolicy.values())
                        .map(p -> RuleRefOption.builder().code(p.getCode()).label(p.getCode()).build())
                        .toList())
                .build();
    }

    private static int nz(Integer v) {
        return v != null ? v : 0;
    }

    /**
     * Возвращает результат операции "get" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public ActiveEffectAdminResponse get(UUID ruleId) {
        return definitionRepository.findByFeatureRuleId(ruleId).stream().findFirst().map(this::toResponse).orElse(null);
    }

    /**
     * Выполняет операции "upsert" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @param req входящее значение req, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public ActiveEffectAdminResponse upsert(UUID ruleId, ActiveEffectEditRequest req) {
        FeatureRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Правило не найдено"));
        FeatureEffectDefinition def = definitionRepository.findByFeatureRuleId(ruleId).stream()
                .findFirst()
                .orElseGet(() -> FeatureEffectDefinition.builder().featureRuleId(rule.getId()).build());

        def.setEffectKey(req.getEffectKey().trim());
        def.setDisplayName(blankToNull(req.getDisplayName()));
        def.setDurationFormulaId(formulaHelper.upsert(def.getDurationFormulaId(), req.getDurationFormula(),
                FormulaResultType.DURATION.getCode()));
        def.setDurationUnitId(req.getDurationUnitId());
        def.setConcentrationRequired(req.isConcentrationRequired());
        def.setStackingPolicy(EffectStackingPolicy.fromCode(req.getStackingPolicy())
                .map(EffectStackingPolicy::getCode).orElse(EffectStackingPolicy.STACK.getCode()));
        def.setActiveEffectGroup(blankToNull(req.getActiveEffectGroup()));
        def.setTargetTypeId(req.getTargetTypeId());
        FeatureEffectDefinition saved = definitionRepository.save(def);

        // Replace modifiers wholesale.
        modifierRepository.deleteAll(modifierRepository.findByEffectDefinitionId(saved.getId()));
        if (req.getModifiers() != null) {
            for (ActiveEffectEditRequest.Modifier m : req.getModifiers()) {
                if (m == null || m.getModifierType() == null || m.getModifierType().isBlank()) {
                    continue;
                }
                modifierRepository.save(FeatureEffectModifier.builder()
                        .effectDefinitionId(saved.getId())
                        .modifierType(m.getModifierType().trim())
                        .valueFormulaId(formulaHelper.upsert(null, m.getValueFormula(), FormulaResultType.INTEGER.getCode()))
                        .damageTypeId(m.getDamageTypeId())
                        .build());
            }
        }

        // Replace end conditions wholesale.
        endConditionRepository.deleteAll(endConditionRepository.findByEffectDefinitionId(saved.getId()));
        if (req.getEndConditions() != null) {
            for (ActiveEffectEditRequest.EndCondition ec : req.getEndConditions()) {
                if (ec == null) {
                    continue;
                }
                boolean empty = ec.getTriggerEventTypeId() == null && ec.getRestTypeId() == null
                        && !ec.isSameFeatureReuse() && (ec.getPredicateFormula() == null || ec.getPredicateFormula().isBlank());
                if (empty) {
                    continue;
                }
                endConditionRepository.save(FeatureEffectEndCondition.builder()
                        .effectDefinitionId(saved.getId())
                        .triggerEventTypeId(ec.getTriggerEventTypeId())
                        .sameFeatureReuse(ec.isSameFeatureReuse())
                        .restTypeId(ec.getRestTypeId())
                        .predicateFormulaId(formulaHelper.upsert(null, ec.getPredicateFormula(),
                                FormulaResultType.BOOLEAN.getCode()))
                        .build());
            }
        }

        return toResponse(saved);
    }

    private ActiveEffectAdminResponse toResponse(FeatureEffectDefinition def) {
        FeatureFormula duration = formulaHelper.find(def.getDurationFormulaId());

        List<ActiveEffectAdminResponse.Modifier> modifiers = new ArrayList<>();
        for (FeatureEffectModifier m : modifierRepository.findByEffectDefinitionId(def.getId())) {
            FeatureFormula value = formulaHelper.find(m.getValueFormulaId());
            modifiers.add(ActiveEffectAdminResponse.Modifier.builder()
                    .id(m.getId())
                    .modifierType(m.getModifierType())
                    .valueFormula(value != null ? value.getExpression() : null)
                    .valueFormulaStatus(value != null ? value.getValidationStatus() : null)
                    .valueFormulaMessage(value != null ? value.getValidationMessage() : null)
                    .damageTypeId(m.getDamageTypeId())
                    .build());
        }

        List<ActiveEffectAdminResponse.EndCondition> endConditions = new ArrayList<>();
        for (FeatureEffectEndCondition ec : endConditionRepository.findByEffectDefinitionId(def.getId())) {
            FeatureFormula pred = formulaHelper.find(ec.getPredicateFormulaId());
            endConditions.add(ActiveEffectAdminResponse.EndCondition.builder()
                    .id(ec.getId())
                    .triggerEventTypeId(ec.getTriggerEventTypeId())
                    .sameFeatureReuse(ec.isSameFeatureReuse())
                    .restTypeId(ec.getRestTypeId())
                    .predicateFormula(pred != null ? pred.getExpression() : null)
                    .predicateFormulaStatus(pred != null ? pred.getValidationStatus() : null)
                    .predicateFormulaMessage(pred != null ? pred.getValidationMessage() : null)
                    .build());
        }

        return ActiveEffectAdminResponse.builder()
                .definitionId(def.getId())
                .effectKey(def.getEffectKey())
                .displayName(def.getDisplayName())
                .durationFormula(duration != null ? duration.getExpression() : null)
                .durationFormulaStatus(duration != null ? duration.getValidationStatus() : null)
                .durationFormulaMessage(duration != null ? duration.getValidationMessage() : null)
                .durationUnitId(def.getDurationUnitId())
                .concentrationRequired(def.isConcentrationRequired())
                .stackingPolicy(def.getStackingPolicy())
                .activeEffectGroup(def.getActiveEffectGroup())
                .targetTypeId(def.getTargetTypeId())
                .modifiers(modifiers)
                .endConditions(endConditions)
                .build();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
