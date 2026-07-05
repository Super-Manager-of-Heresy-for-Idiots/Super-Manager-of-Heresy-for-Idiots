package com.dnd.app.service;

import com.dnd.app.domain.featurerule.FeatureAllowedMonsterFilter;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureSpellGrant;
import com.dnd.app.domain.featurerule.FeatureTrigger;
import com.dnd.app.domain.featurerule.FormulaResultType;
import com.dnd.app.dto.featurerule.MonsterFormAdminResponse;
import com.dnd.app.dto.featurerule.MonsterFormEditRequest;
import com.dnd.app.dto.featurerule.SpellGrantAdminResponse;
import com.dnd.app.dto.featurerule.SpellGrantEditRequest;
import com.dnd.app.dto.featurerule.TriggerAdminResponse;
import com.dnd.app.dto.featurerule.TriggerEditRequest;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.FeatureAllowedMonsterFilterRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import com.dnd.app.repository.FeatureSpellGrantRepository;
import com.dnd.app.repository.FeatureTriggerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Admin CRUD for the three "advanced" feature rules: MONSTER_FORM, TRIGGER_REACTION, SPELL_GRANT. */
@Service
@RequiredArgsConstructor
public class FeatureFormsRuleAdminService {

    private final FeatureRuleRepository ruleRepository;
    private final FeatureAllowedMonsterFilterRepository monsterFilterRepository;
    private final FeatureTriggerRepository triggerRepository;
    private final FeatureSpellGrantRepository spellGrantRepository;
    private final FeatureFormulaAdminHelper formulaHelper;

    private FeatureRule requireRule(UUID ruleId) {
        return ruleRepository.findById(ruleId).orElseThrow(() -> new ResourceNotFoundException("Правило не найдено"));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    // ── MONSTER_FORM ──
    @Transactional(readOnly = true)
    public MonsterFormAdminResponse getMonsterForm(UUID ruleId) {
        return monsterFilterRepository.findByFeatureRuleId(ruleId).stream().findFirst().map(this::toForm).orElse(null);
    }

    @Transactional
    public MonsterFormAdminResponse upsertMonsterForm(UUID ruleId, MonsterFormEditRequest req) {
        FeatureRule rule = requireRule(ruleId);
        FeatureAllowedMonsterFilter f = monsterFilterRepository.findByFeatureRuleId(ruleId).stream()
                .findFirst().orElseGet(() -> FeatureAllowedMonsterFilter.builder().featureRuleId(rule.getId()).build());
        f.setCreatureType(blankToNull(req.getCreatureType()));
        f.setMaxCrFormulaId(formulaHelper.upsert(f.getMaxCrFormulaId(), req.getMaxCrFormula(),
                FormulaResultType.DECIMAL.getCode()));
        f.setMovementRestriction(blankToNull(req.getMovementRestriction()));
        f.setSizeFilter(blankToNull(req.getSizeFilter()));
        f.setSourceFilter(blankToNull(req.getSourceFilter()));
        return toForm(monsterFilterRepository.save(f));
    }

    private MonsterFormAdminResponse toForm(FeatureAllowedMonsterFilter f) {
        FeatureFormula cr = formulaHelper.find(f.getMaxCrFormulaId());
        return MonsterFormAdminResponse.builder()
                .id(f.getId())
                .creatureType(f.getCreatureType())
                .maxCrFormula(cr != null ? cr.getExpression() : null)
                .maxCrFormulaStatus(cr != null ? cr.getValidationStatus() : null)
                .maxCrFormulaMessage(cr != null ? cr.getValidationMessage() : null)
                .movementRestriction(f.getMovementRestriction())
                .sizeFilter(f.getSizeFilter())
                .sourceFilter(f.getSourceFilter())
                .build();
    }

    // ── TRIGGER_REACTION ──
    @Transactional(readOnly = true)
    public TriggerAdminResponse getTrigger(UUID ruleId) {
        return triggerRepository.findByFeatureRuleId(ruleId).stream().findFirst().map(this::toTrigger).orElse(null);
    }

    @Transactional
    public TriggerAdminResponse upsertTrigger(UUID ruleId, TriggerEditRequest req) {
        FeatureRule rule = requireRule(ruleId);
        FeatureTrigger tr = triggerRepository.findByFeatureRuleId(ruleId).stream()
                .findFirst().orElseGet(() -> FeatureTrigger.builder().featureRuleId(rule.getId()).build());
        tr.setEventTypeId(req.getEventTypeId());
        tr.setTiming(blankToNull(req.getTiming()));
        tr.setPredicateFormulaId(formulaHelper.upsert(tr.getPredicateFormulaId(), req.getPredicateFormula(),
                FormulaResultType.BOOLEAN.getCode()));
        tr.setRequiresPlayerConfirmation(req.isRequiresPlayerConfirmation());
        tr.setConsumesReaction(req.isConsumesReaction());
        return toTrigger(triggerRepository.save(tr));
    }

    private TriggerAdminResponse toTrigger(FeatureTrigger tr) {
        FeatureFormula pred = formulaHelper.find(tr.getPredicateFormulaId());
        return TriggerAdminResponse.builder()
                .id(tr.getId())
                .eventTypeId(tr.getEventTypeId())
                .timing(tr.getTiming())
                .predicateFormula(pred != null ? pred.getExpression() : null)
                .predicateFormulaStatus(pred != null ? pred.getValidationStatus() : null)
                .predicateFormulaMessage(pred != null ? pred.getValidationMessage() : null)
                .requiresPlayerConfirmation(tr.isRequiresPlayerConfirmation())
                .consumesReaction(tr.isConsumesReaction())
                .build();
    }

    // ── SPELL_GRANT ──
    @Transactional(readOnly = true)
    public SpellGrantAdminResponse getSpellGrant(UUID ruleId) {
        return spellGrantRepository.findByFeatureRuleId(ruleId).stream().findFirst().map(this::toGrant).orElse(null);
    }

    @Transactional
    public SpellGrantAdminResponse upsertSpellGrant(UUID ruleId, SpellGrantEditRequest req) {
        FeatureRule rule = requireRule(ruleId);
        FeatureSpellGrant g = spellGrantRepository.findByFeatureRuleId(ruleId).stream()
                .findFirst().orElseGet(() -> FeatureSpellGrant.builder().featureRuleId(rule.getId()).build());
        g.setSpellId(req.getSpellId());
        g.setCountsAgainstKnown(req.isCountsAgainstKnown());
        g.setAlwaysPrepared(req.isAlwaysPrepared());
        g.setCastWithoutSlot(req.isCastWithoutSlot());
        g.setSpellcastingAbilityOverrideId(req.getSpellcastingAbilityOverrideId());
        return toGrant(spellGrantRepository.save(g));
    }

    private SpellGrantAdminResponse toGrant(FeatureSpellGrant g) {
        return SpellGrantAdminResponse.builder()
                .id(g.getId())
                .spellId(g.getSpellId())
                .countsAgainstKnown(g.isCountsAgainstKnown())
                .alwaysPrepared(g.isAlwaysPrepared())
                .castWithoutSlot(g.isCastWithoutSlot())
                .spellcastingAbilityOverrideId(g.getSpellcastingAbilityOverrideId())
                .build();
    }
}
