package com.dnd.app.service;

import com.dnd.app.domain.featurerule.FeatureCompanionDefinition;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleProfile;
import com.dnd.app.domain.featurerule.FormulaResultType;
import com.dnd.app.dto.featurerule.CompanionDefinitionAdminResponse;
import com.dnd.app.dto.featurerule.CompanionDefinitionEditRequest;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.FeatureCompanionDefinitionRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Admin replace-style editor for COMPANION rule definitions. */
@Service
@RequiredArgsConstructor
public class FeatureCompanionDefinitionAdminService {

    private final FeatureRuleRepository ruleRepository;
    private final FeatureCompanionDefinitionRepository definitionRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureFormulaAdminHelper formulaHelper;

    @Transactional(readOnly = true)
    public CompanionDefinitionAdminResponse get(UUID ruleId) {
        requireRule(ruleId);
        return toResponse(ruleId);
    }

    @Transactional
    public CompanionDefinitionAdminResponse replace(UUID ruleId, CompanionDefinitionEditRequest req) {
        FeatureRule rule = requireRule(ruleId);
        List<UUID> oldFormulaIds = definitionRepository.findByFeatureRuleIdOrderBySortOrderAsc(rule.getId()).stream()
                .flatMap(def -> java.util.stream.Stream.of(
                        def.getHpFormulaId(),
                        def.getAcFormulaId(),
                        def.getAttackBonusFormulaId()))
                .filter(java.util.Objects::nonNull)
                .toList();
        definitionRepository.deleteByFeatureRuleId(rule.getId());
        if (!oldFormulaIds.isEmpty()) {
            formulaRepository.deleteAllById(oldFormulaIds);
        }

        int index = 0;
        for (CompanionDefinitionEditRequest.Companion row : safe(req.getCompanions())) {
            String key = required(row.getCompanionKey(), "Ключ спутника обязателен");
            definitionRepository.save(FeatureCompanionDefinition.builder()
                    .featureRuleId(rule.getId())
                    .companionKey(key)
                    .monsterId(row.getMonsterId())
                    .nameTemplate(blankToNull(row.getNameTemplate()))
                    .hpFormulaId(formulaHelper.upsert(null, row.getHpFormula(), FormulaResultType.INTEGER.getCode()))
                    .acFormulaId(formulaHelper.upsert(null, row.getAcFormula(), FormulaResultType.INTEGER.getCode()))
                    .attackBonusFormulaId(formulaHelper.upsert(null, row.getAttackBonusFormula(),
                            FormulaResultType.INTEGER.getCode()))
                    .summonTiming(blankToNull(row.getSummonTiming()))
                    .sortOrder(row.getSortOrder() != null ? row.getSortOrder() : index)
                    .notes(blankToNull(row.getNotes()))
                    .build());
            index++;
        }
        return toResponse(rule.getId());
    }

    private FeatureRule requireRule(UUID ruleId) {
        FeatureRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Правило не найдено"));
        if (!FeatureRuleProfile.COMPANION.getCode().equals(rule.getRuleType())) {
            throw new BadRequestException("Редактор спутников доступен только для companion правил");
        }
        return rule;
    }

    private CompanionDefinitionAdminResponse toResponse(UUID ruleId) {
        List<CompanionDefinitionAdminResponse.Companion> rows = definitionRepository
                .findByFeatureRuleIdOrderBySortOrderAsc(ruleId)
                .stream()
                .map(def -> {
                    FeatureFormula hp = formulaHelper.find(def.getHpFormulaId());
                    FeatureFormula ac = formulaHelper.find(def.getAcFormulaId());
                    FeatureFormula attack = formulaHelper.find(def.getAttackBonusFormulaId());
                    return CompanionDefinitionAdminResponse.Companion.builder()
                            .id(def.getId())
                            .companionKey(def.getCompanionKey())
                            .monsterId(def.getMonsterId())
                            .nameTemplate(def.getNameTemplate())
                            .hpFormula(hp != null ? hp.getExpression() : null)
                            .hpFormulaStatus(hp != null ? hp.getValidationStatus() : null)
                            .hpFormulaMessage(hp != null ? hp.getValidationMessage() : null)
                            .acFormula(ac != null ? ac.getExpression() : null)
                            .acFormulaStatus(ac != null ? ac.getValidationStatus() : null)
                            .acFormulaMessage(ac != null ? ac.getValidationMessage() : null)
                            .attackBonusFormula(attack != null ? attack.getExpression() : null)
                            .attackBonusFormulaStatus(attack != null ? attack.getValidationStatus() : null)
                            .attackBonusFormulaMessage(attack != null ? attack.getValidationMessage() : null)
                            .summonTiming(def.getSummonTiming())
                            .sortOrder(def.getSortOrder())
                            .notes(def.getNotes())
                            .build();
                })
                .toList();
        return CompanionDefinitionAdminResponse.builder().companions(rows).build();
    }

    private static String required(String raw, String message) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException(message);
        }
        return raw.trim();
    }

    private static String blankToNull(String raw) {
        return raw == null || raw.isBlank() ? null : raw.trim();
    }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
