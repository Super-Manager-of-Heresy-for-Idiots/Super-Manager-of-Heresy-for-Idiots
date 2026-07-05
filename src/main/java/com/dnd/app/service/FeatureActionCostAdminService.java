package com.dnd.app.service;

import com.dnd.app.domain.featurerule.ActionType;
import com.dnd.app.domain.featurerule.FeatureActionCost;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FormulaResultType;
import com.dnd.app.dto.featurerule.ActionCostAdminResponse;
import com.dnd.app.dto.featurerule.ActionCostEditRequest;
import com.dnd.app.dto.featurerule.ActionTypeOption;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.ActionTypeRepository;
import com.dnd.app.repository.FeatureActionCostRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Admin CRUD for a feature ACTION_COST rule (Rule Workbench action-cost editor). */
@Service
@RequiredArgsConstructor
public class FeatureActionCostAdminService {

    private final FeatureRuleRepository ruleRepository;
    private final FeatureActionCostRepository actionCostRepository;
    private final ActionTypeRepository actionTypeRepository;
    private final FeatureFormulaAdminHelper formulaHelper;

    @Transactional(readOnly = true)
    public List<ActionTypeOption> listActionTypes() {
        return actionTypeRepository.findAll().stream()
                .sorted(Comparator.comparing(ActionType::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(a -> ActionTypeOption.builder().id(a.getId()).code(a.getCode()).label(a.getDisplayName()).build())
                .toList();
    }

    @Transactional(readOnly = true)
    public ActionCostAdminResponse get(UUID ruleId) {
        return actionCostRepository.findByFeatureRuleId(ruleId).stream().findFirst().map(this::toResponse).orElse(null);
    }

    @Transactional
    public ActionCostAdminResponse upsert(UUID ruleId, ActionCostEditRequest req) {
        FeatureRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Правило не найдено"));
        if (req.getActionTypeId() == null || actionTypeRepository.findById(req.getActionTypeId()).isEmpty()) {
            throw new BadRequestException("Укажите корректный тип действия");
        }
        FeatureActionCost cost = actionCostRepository.findByFeatureRuleId(ruleId).stream()
                .findFirst()
                .orElseGet(() -> FeatureActionCost.builder().featureRuleId(rule.getId()).build());

        cost.setActionTypeId(req.getActionTypeId());
        cost.setAmount(req.getAmount() != null ? req.getAmount() : 1);
        cost.setConditionFormulaId(formulaHelper.upsert(cost.getConditionFormulaId(), req.getConditionFormula(),
                FormulaResultType.BOOLEAN.getCode()));

        return toResponse(actionCostRepository.save(cost));
    }

    private ActionCostAdminResponse toResponse(FeatureActionCost cost) {
        FeatureFormula cond = formulaHelper.find(cost.getConditionFormulaId());
        return ActionCostAdminResponse.builder()
                .id(cost.getId())
                .actionTypeId(cost.getActionTypeId())
                .amount(cost.getAmount())
                .conditionFormula(cond != null ? cond.getExpression() : null)
                .conditionFormulaStatus(cond != null ? cond.getValidationStatus() : null)
                .conditionFormulaMessage(cond != null ? cond.getValidationMessage() : null)
                .build();
    }
}
