package com.dnd.app.service;

import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FormulaRoundingMode;
import com.dnd.app.repository.FeatureFormulaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс FeatureFormulaAdminHelper описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Component
@RequiredArgsConstructor
public class FeatureFormulaAdminHelper {

    private final FeatureFormulaRepository formulaRepository;
    private final FeatureFormulaService formulaService;

    /**
     * Выполняет операции "upsert" в рамках бизнес-логики домена.
     * @param existingId идентификатор existing, используемый для выбора нужного бизнес-объекта
     * @param expression входящее значение expression, используемое бизнес-сценарием
     * @param resultType входящее значение result type, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Находит результат операции "find" в рамках бизнес-логики домена.
     * @param formulaId идентификатор formula, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    public FeatureFormula find(UUID formulaId) {
        return formulaId == null ? null : formulaRepository.findById(formulaId).orElse(null);
    }
}
