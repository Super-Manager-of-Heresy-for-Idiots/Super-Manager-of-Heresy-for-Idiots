package com.dnd.app.service;

import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FormulaResultType;
import com.dnd.app.domain.featurerule.FormulaRoundingMode;
import com.dnd.app.domain.featurerule.RestType;
import com.dnd.app.dto.featurerule.ResourceDefinitionAdminResponse;
import com.dnd.app.dto.featurerule.ResourceDefinitionEditRequest;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureResourceDefinitionRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import com.dnd.app.repository.RestTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Admin CRUD for a feature RESOURCE rule's definition (Rule Workbench resource editor): resource key,
 * display name, the max-value DSL formula (validated + stamped), reset window and pooling. This is the piece
 * that lets a GM/admin actually author "how many charges and by what formula" — e.g. {@code ability_mod("INT")}.
 */
@Service
@RequiredArgsConstructor
public class FeatureResourceDefinitionAdminService {

    private final FeatureRuleRepository ruleRepository;
    private final FeatureResourceDefinitionRepository definitionRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final RestTypeRepository restTypeRepository;
    private final FeatureFormulaService formulaService;

    @Transactional(readOnly = true)
    public ResourceDefinitionAdminResponse get(UUID ruleId) {
        return definitionRepository.findByFeatureRuleId(ruleId).stream()
                .findFirst()
                .map(this::toResponse)
                .orElse(null);
    }

    /** Distinct resource keys already defined — powers the workbench autocomplete for keys and formula args. */
    @Transactional(readOnly = true)
    public java.util.List<String> listResourceKeys() {
        return definitionRepository.findDistinctResourceKeys();
    }

    @Transactional
    public ResourceDefinitionAdminResponse upsert(UUID ruleId, ResourceDefinitionEditRequest req) {
        FeatureRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Правило не найдено"));

        String key = req.getResourceKey() != null ? req.getResourceKey().trim() : null;
        if (key == null || key.isBlank()) {
            throw new BadRequestException("Ключ ресурса обязателен");
        }

        FeatureResourceDefinition def = definitionRepository.findByFeatureRuleId(ruleId).stream()
                .findFirst()
                .orElseGet(() -> FeatureResourceDefinition.builder().featureRuleId(rule.getId()).build());

        def.setResourceKey(key);
        def.setDisplayName(blankToNull(req.getDisplayName()));
        def.setAllowNegative(req.isAllowNegative());
        def.setSharedPoolKey(blankToNull(req.getSharedPoolKey()));

        // Max value formula: create/update the FeatureFormula (validated + stamped), or clear it.
        String expr = req.getMaxFormula() != null ? req.getMaxFormula().trim() : null;
        if (expr != null && !expr.isBlank()) {
            FeatureFormula formula = def.getMaxFormulaId() != null
                    ? formulaRepository.findById(def.getMaxFormulaId()).orElseGet(FeatureFormula::new)
                    : new FeatureFormula();
            formula.setExpression(expr);
            formula.setResultType(FormulaResultType.INTEGER.getCode());
            if (formula.getRoundingMode() == null) {
                formula.setRoundingMode(FormulaRoundingMode.NONE.getCode());
            }
            formulaService.validateAndStamp(formula);
            def.setMaxFormulaId(formulaRepository.save(formula).getId());
        } else {
            def.setMaxFormulaId(null);
        }

        // Reset window (rest type code -> id).
        if (req.getResetRestType() != null && !req.getResetRestType().isBlank()) {
            def.setResetRestTypeId(restTypeRepository.findByCode(req.getResetRestType().trim())
                    .map(RestType::getId).orElse(null));
        } else {
            def.setResetRestTypeId(null);
        }

        return toResponse(definitionRepository.save(def));
    }

    private ResourceDefinitionAdminResponse toResponse(FeatureResourceDefinition def) {
        String expr = null;
        String status = null;
        String message = null;
        if (def.getMaxFormulaId() != null) {
            FeatureFormula f = formulaRepository.findById(def.getMaxFormulaId()).orElse(null);
            if (f != null) {
                expr = f.getExpression();
                status = f.getValidationStatus();
                message = f.getValidationMessage();
            }
        }
        String restCode = def.getResetRestTypeId() == null ? null
                : restTypeRepository.findById(def.getResetRestTypeId()).map(RestType::getCode).orElse(null);

        return ResourceDefinitionAdminResponse.builder()
                .id(def.getId())
                .featureRuleId(def.getFeatureRuleId())
                .resourceKey(def.getResourceKey())
                .displayName(def.getDisplayName())
                .maxFormula(expr)
                .maxFormulaStatus(status)
                .maxFormulaMessage(message)
                .resetRestType(restCode)
                .allowNegative(def.isAllowNegative())
                .sharedPoolKey(def.getSharedPoolKey())
                .build();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
