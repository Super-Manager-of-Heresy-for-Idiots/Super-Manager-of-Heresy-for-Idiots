package com.dnd.app.service;

import com.dnd.app.domain.featurerule.FeatureDamageRule;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FormulaResultType;
import com.dnd.app.dto.featurerule.DamageRuleAdminResponse;
import com.dnd.app.dto.featurerule.DamageRuleEditRequest;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.FeatureDamageRuleRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Admin CRUD for a feature DAMAGE rule (Rule Workbench damage editor). */
@Service
@RequiredArgsConstructor
public class FeatureDamageRuleAdminService {

    private final FeatureRuleRepository ruleRepository;
    private final FeatureDamageRuleRepository damageRepository;
    private final FeatureFormulaAdminHelper formulaHelper;

    @Transactional(readOnly = true)
    public DamageRuleAdminResponse get(UUID ruleId) {
        return damageRepository.findByFeatureRuleId(ruleId).stream().findFirst().map(this::toResponse).orElse(null);
    }

    @Transactional
    public DamageRuleAdminResponse upsert(UUID ruleId, DamageRuleEditRequest req) {
        FeatureRule rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new ResourceNotFoundException("Правило не найдено"));
        FeatureDamageRule dmg = damageRepository.findByFeatureRuleId(ruleId).stream()
                .findFirst()
                .orElseGet(() -> FeatureDamageRule.builder().featureRuleId(rule.getId()).build());

        dmg.setDiceFormulaId(formulaHelper.upsert(dmg.getDiceFormulaId(), req.getDiceFormula(),
                FormulaResultType.DICE.getCode()));
        dmg.setFlatAmountFormulaId(formulaHelper.upsert(dmg.getFlatAmountFormulaId(), req.getFlatFormula(),
                FormulaResultType.INTEGER.getCode()));
        dmg.setDamageTypeId(req.getDamageTypeId());
        dmg.setRequiresAttackHit(req.isRequiresAttackHit());
        dmg.setRequiresSave(req.isRequiresSave());
        dmg.setHalfOnSave(req.isHalfOnSave());

        return toResponse(damageRepository.save(dmg));
    }

    private DamageRuleAdminResponse toResponse(FeatureDamageRule dmg) {
        FeatureFormula dice = formulaHelper.find(dmg.getDiceFormulaId());
        FeatureFormula flat = formulaHelper.find(dmg.getFlatAmountFormulaId());
        return DamageRuleAdminResponse.builder()
                .id(dmg.getId())
                .diceFormula(dice != null ? dice.getExpression() : null)
                .diceFormulaStatus(dice != null ? dice.getValidationStatus() : null)
                .diceFormulaMessage(dice != null ? dice.getValidationMessage() : null)
                .flatFormula(flat != null ? flat.getExpression() : null)
                .flatFormulaStatus(flat != null ? flat.getValidationStatus() : null)
                .flatFormulaMessage(flat != null ? flat.getValidationMessage() : null)
                .damageTypeId(dmg.getDamageTypeId())
                .requiresAttackHit(dmg.isRequiresAttackHit())
                .requiresSave(dmg.isRequiresSave())
                .halfOnSave(dmg.isHalfOnSave())
                .build();
    }
}
