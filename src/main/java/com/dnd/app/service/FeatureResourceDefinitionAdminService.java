package com.dnd.app.service;

import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureResourceDefinition;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FormulaResultType;
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
 * Класс FeatureResourceDefinitionAdminService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class FeatureResourceDefinitionAdminService {

    private final FeatureRuleRepository ruleRepository;
    private final FeatureResourceDefinitionRepository definitionRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final RestTypeRepository restTypeRepository;
    private final FeatureFormulaService formulaService;
    private final FeatureFormulaAdminHelper formulaHelper;

    /**
     * Возвращает результат операции "get" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public ResourceDefinitionAdminResponse get(UUID ruleId) {
        return definitionRepository.findByFeatureRuleId(ruleId).stream()
                .findFirst()
                .map(this::toResponse)
                .orElse(null);
    }

    /**
     * Выполняет операции "upsert" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @param req входящее значение req, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
    /**
     * Возвращает список для операции "list resource keys" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public java.util.List<String> listResourceKeys() {
        return definitionRepository.findDistinctResourceKeys();
    }

    /**
     * Выполняет операции "upsert" в рамках бизнес-логики домена.
     * @param ruleId идентификатор rule, используемый для выбора нужного бизнес-объекта
     * @param req входящее значение req, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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

        def.setMaxFormulaId(formulaHelper.upsert(def.getMaxFormulaId(), req.getMaxFormula(),
                FormulaResultType.INTEGER.getCode()));
        def.setResetAmountFormulaId(formulaHelper.upsert(def.getResetAmountFormulaId(),
                req.getResetAmountFormula(), FormulaResultType.INTEGER.getCode()));
        def.setSpendPerUseFormulaId(formulaHelper.upsert(def.getSpendPerUseFormulaId(),
                req.getSpendPerUseFormula(), FormulaResultType.INTEGER.getCode()));

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
        FeatureFormula max = formulaHelper.find(def.getMaxFormulaId());
        FeatureFormula reset = formulaHelper.find(def.getResetAmountFormulaId());
        FeatureFormula spend = formulaHelper.find(def.getSpendPerUseFormulaId());
        String restCode = def.getResetRestTypeId() == null ? null
                : restTypeRepository.findById(def.getResetRestTypeId()).map(RestType::getCode).orElse(null);

        return ResourceDefinitionAdminResponse.builder()
                .id(def.getId())
                .featureRuleId(def.getFeatureRuleId())
                .resourceKey(def.getResourceKey())
                .displayName(def.getDisplayName())
                .maxFormula(max != null ? max.getExpression() : null)
                .maxFormulaStatus(max != null ? max.getValidationStatus() : null)
                .maxFormulaMessage(max != null ? max.getValidationMessage() : null)
                .resetRestType(restCode)
                .resetAmountFormula(reset != null ? reset.getExpression() : null)
                .resetAmountFormulaStatus(reset != null ? reset.getValidationStatus() : null)
                .resetAmountFormulaMessage(reset != null ? reset.getValidationMessage() : null)
                .spendPerUseFormula(spend != null ? spend.getExpression() : null)
                .spendPerUseFormulaStatus(spend != null ? spend.getValidationStatus() : null)
                .spendPerUseFormulaMessage(spend != null ? spend.getValidationMessage() : null)
                .allowNegative(def.isAllowNegative())
                .sharedPoolKey(def.getSharedPoolKey())
                .build();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
