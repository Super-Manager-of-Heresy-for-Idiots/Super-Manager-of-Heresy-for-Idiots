package com.dnd.app.service;

import com.dnd.app.domain.featurerule.*;
import com.dnd.app.dto.featurerule.GenericFormulaRuleAdminResponse;
import com.dnd.app.dto.featurerule.GenericFormulaRuleEditRequest;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureRuleFormulaRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Класс FeatureGenericFormulaRuleAdminService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class FeatureGenericFormulaRuleAdminService {

    private final FeatureRuleRepository ruleRepository;
    private final FeatureRuleFormulaRepository ruleFormulaRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureFormulaService formulaService;

    /**
     * Возвращает результат операции "get" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public GenericFormulaRuleAdminResponse get(UUID ruleId) {
        requireRule(ruleId);
        return toResponse(ruleId);
    }

    /**
     * Выполняет операции "replace" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @param req входящее значение req, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public GenericFormulaRuleAdminResponse replace(UUID ruleId, GenericFormulaRuleEditRequest req) {
        FeatureRule rule = requireRule(ruleId);
        List<UUID> oldFormulaIds = ruleFormulaRepository.findByFeatureRuleIdOrderBySortOrderAsc(rule.getId()).stream()
                .map(FeatureRuleFormula::getFormulaId)
                .toList();
        ruleFormulaRepository.deleteByFeatureRuleId(rule.getId());
        if (!oldFormulaIds.isEmpty()) {
            formulaRepository.deleteAllById(oldFormulaIds);
        }

        int index = 0;
        for (GenericFormulaRuleEditRequest.FormulaRow row : safe(req.getFormulas())) {
            String key = required(row.getFormulaKey(), "Ключ формулы обязателен");
            String expression = required(row.getExpression(), "Выражение формулы обязательно");
            String resultType = row.getResultType() == null || row.getResultType().isBlank()
                    ? FormulaResultType.INTEGER.getCode()
                    : row.getResultType().trim();
            if (FormulaResultType.fromCode(resultType).isEmpty()) {
                throw new BadRequestException("Неизвестный тип результата формулы: " + resultType);
            }
            String rounding = row.getRoundingMode() == null || row.getRoundingMode().isBlank()
                    ? FormulaRoundingMode.NONE.getCode()
                    : row.getRoundingMode().trim();
            if (FormulaRoundingMode.fromCode(rounding).isEmpty()) {
                throw new BadRequestException("Неизвестное округление формулы: " + rounding);
            }
            FeatureFormula formula = FeatureFormula.builder()
                    .expression(expression)
                    .expressionType(blankToNull(row.getExpressionType()))
                    .resultType(resultType)
                    .roundingMode(rounding)
                    .minValue(row.getMinValue())
                    .maxValue(row.getMaxValue())
                    .build();
            formulaService.validateAndStamp(formula);
            formula = formulaRepository.save(formula);
            ruleFormulaRepository.save(FeatureRuleFormula.builder()
                    .featureRuleId(rule.getId())
                    .formulaId(formula.getId())
                    .formulaKey(key)
                    .label(blankToNull(row.getLabel()))
                    .sortOrder(row.getSortOrder() != null ? row.getSortOrder() : index)
                    .build());
            index++;
        }
        return toResponse(rule.getId());
    }

    private FeatureRule requireRule(UUID ruleId) {
        FeatureRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Правило не найдено"));
        if (!FeatureRuleProfile.FORMULA.getCode().equals(rule.getRuleType())
                && !FeatureRuleProfile.MANUAL_ADJUDICATION.getCode().equals(rule.getRuleType())) {
            throw new BadRequestException("Редактор формул доступен только для formula/manual_adjudication правил");
        }
        return rule;
    }

    private GenericFormulaRuleAdminResponse toResponse(UUID ruleId) {
        List<GenericFormulaRuleAdminResponse.FormulaRow> rows = ruleFormulaRepository
                .findByFeatureRuleIdOrderBySortOrderAsc(ruleId)
                .stream()
                .map(link -> {
                    FeatureFormula formula = formulaRepository.findById(link.getFormulaId()).orElse(null);
                    return GenericFormulaRuleAdminResponse.FormulaRow.builder()
                            .id(link.getId())
                            .formulaKey(link.getFormulaKey())
                            .label(link.getLabel())
                            .expression(formula != null ? formula.getExpression() : null)
                            .expressionType(formula != null ? formula.getExpressionType() : null)
                            .resultType(formula != null ? formula.getResultType() : null)
                            .roundingMode(formula != null ? formula.getRoundingMode() : null)
                            .minValue(formula != null ? formula.getMinValue() : null)
                            .maxValue(formula != null ? formula.getMaxValue() : null)
                            .validationStatus(formula != null ? formula.getValidationStatus() : null)
                            .validationMessage(formula != null ? formula.getValidationMessage() : null)
                            .sortOrder(link.getSortOrder())
                            .build();
                })
                .toList();
        return GenericFormulaRuleAdminResponse.builder().formulas(rows).build();
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
