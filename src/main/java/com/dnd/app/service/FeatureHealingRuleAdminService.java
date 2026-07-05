package com.dnd.app.service;

import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureHealingRule;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FormulaResultType;
import com.dnd.app.domain.featurerule.TargetType;
import com.dnd.app.dto.featurerule.HealingRuleAdminResponse;
import com.dnd.app.dto.featurerule.HealingRuleEditRequest;
import com.dnd.app.dto.featurerule.TargetTypeOption;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.FeatureHealingRuleRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import com.dnd.app.repository.TargetTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Admin CRUD for a feature HEALING rule (Rule Workbench healing editor). */
@Service
@RequiredArgsConstructor
public class FeatureHealingRuleAdminService {

    private final FeatureRuleRepository ruleRepository;
    private final FeatureHealingRuleRepository healingRepository;
    private final TargetTypeRepository targetTypeRepository;
    private final FeatureFormulaAdminHelper formulaHelper;

    @Transactional(readOnly = true)
    public List<TargetTypeOption> listTargetTypes() {
        return targetTypeRepository.findAll().stream()
                .sorted(Comparator.comparing(TargetType::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(t -> TargetTypeOption.builder().id(t.getId()).code(t.getCode()).label(t.getDisplayName()).build())
                .toList();
    }

    @Transactional(readOnly = true)
    public HealingRuleAdminResponse get(UUID ruleId) {
        return healingRepository.findByFeatureRuleId(ruleId).stream().findFirst().map(this::toResponse).orElse(null);
    }

    @Transactional
    public HealingRuleAdminResponse upsert(UUID ruleId, HealingRuleEditRequest req) {
        FeatureRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Правило не найдено"));
        FeatureHealingRule heal = healingRepository.findByFeatureRuleId(ruleId).stream()
                .findFirst()
                .orElseGet(() -> FeatureHealingRule.builder().featureRuleId(rule.getId()).build());

        String resultType = "dice".equalsIgnoreCase(req.getAmountFormulaType())
                ? FormulaResultType.DICE.getCode()
                : FormulaResultType.INTEGER.getCode();
        heal.setAmountFormulaId(formulaHelper.upsert(heal.getAmountFormulaId(), req.getAmountFormula(), resultType));
        heal.setTargetTypeId(req.getTargetTypeId());
        heal.setTempHp(req.isTempHp());
        heal.setCanReviveFromZero(req.isCanReviveFromZero());

        return toResponse(healingRepository.save(heal));
    }

    private HealingRuleAdminResponse toResponse(FeatureHealingRule heal) {
        FeatureFormula amount = formulaHelper.find(heal.getAmountFormulaId());
        return HealingRuleAdminResponse.builder()
                .id(heal.getId())
                .amountFormula(amount != null ? amount.getExpression() : null)
                .amountFormulaType(amount != null ? amount.getResultType() : null)
                .amountFormulaStatus(amount != null ? amount.getValidationStatus() : null)
                .amountFormulaMessage(amount != null ? amount.getValidationMessage() : null)
                .targetTypeId(heal.getTargetTypeId())
                .tempHp(heal.isTempHp())
                .canReviveFromZero(heal.isCanReviveFromZero())
                .build();
    }
}
