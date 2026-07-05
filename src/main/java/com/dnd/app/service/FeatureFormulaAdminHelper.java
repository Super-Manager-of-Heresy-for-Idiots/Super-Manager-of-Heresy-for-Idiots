package com.dnd.app.service;

import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FormulaRoundingMode;
import com.dnd.app.repository.FeatureFormulaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Shared helper for the admin rule-mechanics editors: create/update the DSL {@link FeatureFormula} behind an
 * editor field (damage dice, resource max, DC, duration, …) and read it back. Keeps every editor from
 * duplicating the create-validate-stamp-save dance.
 */
@Component
@RequiredArgsConstructor
public class FeatureFormulaAdminHelper {

    private final FeatureFormulaRepository formulaRepository;
    private final FeatureFormulaService formulaService;

    /** Create/update the formula for a field; returns its id, or null when the expression is blank (field cleared). */
    public UUID upsert(UUID existingId, String expression, String resultType) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        FeatureFormula formula = existingId != null
                ? formulaRepository.findById(existingId).orElseGet(FeatureFormula::new)
                : new FeatureFormula();
        formula.setExpression(expression.trim());
        formula.setResultType(resultType);
        if (formula.getRoundingMode() == null) {
            formula.setRoundingMode(FormulaRoundingMode.NONE.getCode());
        }
        formulaService.validateAndStamp(formula);
        return formulaRepository.save(formula).getId();
    }

    /** The stored formula (for reading expression / validation status), or null. */
    public FeatureFormula find(UUID formulaId) {
        return formulaId == null ? null : formulaRepository.findById(formulaId).orElse(null);
    }
}
