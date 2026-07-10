package com.dnd.app.service;

import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.featurerule.FeatureDamageRule;
import com.dnd.app.domain.featurerule.FeatureFormula;
import com.dnd.app.domain.featurerule.FeatureHealingRule;
import com.dnd.app.domain.featurerule.FeatureIssueSeverity;
import com.dnd.app.domain.featurerule.FeatureReviewStatus;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleIssue;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.domain.featurerule.FeatureRuleProfile;
import com.dnd.app.domain.featurerule.FeatureRuleSource;
import com.dnd.app.dto.featurerule.FeatureRuleBackfillResult;
import com.dnd.app.repository.ClassFeatureRepository;
import com.dnd.app.repository.FeatureDamageRuleRepository;
import com.dnd.app.repository.FeatureFormulaRepository;
import com.dnd.app.repository.FeatureHealingRuleRepository;
import com.dnd.app.repository.FeatureRuleIssueRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Класс FeatureRuleBackfillService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeatureRuleBackfillService {

    private static final String OWNER = FeatureRuleOwnerType.CLASS_FEATURE.getCode();

    private final ClassFeatureRepository classFeatureRepository;
    private final FeatureRuleRepository ruleRepository;
    private final FeatureRuleIssueRepository issueRepository;
    private final FeatureFormulaRepository formulaRepository;
    private final FeatureDamageRuleRepository damageRuleRepository;
    private final FeatureHealingRuleRepository healingRuleRepository;
    private final FeatureRuleRevisionService revisionService;

    /**
     * Выполняет обратное заполнение операции "backfill" в рамках бизнес-логики домена.
     * @param apply признак применения изменений вместо пробного расчета
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public FeatureRuleBackfillResult backfill(boolean apply) {
        List<ClassFeature> features = classFeatureRepository.findAll().stream()
                .filter(f -> f.getSubclass() == null)
                .toList();

        int touched = 0;
        int skipped = 0;
        int rulesCreated = 0;
        int issuesCreated = 0;
        int formulasCreated = 0;

        for (ClassFeature feature : features) {
            boolean hasParserRules = ruleRepository
                    .findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(OWNER, feature.getId()).stream()
                    .anyMatch(r -> FeatureRuleSource.PARSER.getCode().equals(r.getSource()));
            if (hasParserRules) {
                skipped++;
                continue;
            }

            List<FeatureRuleProfile> planned = planProfiles(feature);
            boolean warning = Boolean.TRUE.equals(feature.getWarning());
            if (planned.isEmpty() && !warning) {
                continue;
            }
            touched++;

            if (!apply) {
                rulesCreated += planned.size();
                issuesCreated += warning ? 1 : 0;
                formulasCreated += countFormulas(feature, planned);
                continue;
            }

            int sort = 0;
            for (FeatureRuleProfile profile : planned) {
                try {
                    formulasCreated += createRuleFor(feature, profile, sort++);
                    rulesCreated++;
                } catch (RuntimeException e) {
                    log.warn("Backfill failed for feature {} profile {}: {}",
                            feature.getId(), profile.getCode(), e.getMessage());
                }
            }
            if (warning) {
                issueRepository.save(FeatureRuleIssue.builder()
                        .ownerType(OWNER).ownerId(feature.getId())
                        .issueType("ambiguous_parse")
                        .severity(FeatureIssueSeverity.WARN.getCode())
                        .message(feature.getWarningReason() != null
                                ? feature.getWarningReason() : "Автопарс требует проверки")
                        .resolved(false)
                        .build());
                issuesCreated++;
            }
        }

        return FeatureRuleBackfillResult.builder()
                .applied(apply)
                .runtimeFeatures(features.size())
                .featuresTouched(touched)
                .featuresSkipped(skipped)
                .rulesCreated(rulesCreated)
                .issuesCreated(issuesCreated)
                .formulasCreated(formulasCreated)
                .build();
    }

    private List<FeatureRuleProfile> planProfiles(ClassFeature f) {
        List<FeatureRuleProfile> out = new java.util.ArrayList<>();
        if (f.getActivationType() != null && !f.getActivationType().isBlank()
                && !"PASSIVE".equalsIgnoreCase(f.getActivationType())) {
            out.add(FeatureRuleProfile.ACTION_COST);
        }
        if (f.getDamageDice() != null && !f.getDamageDice().isBlank()) {
            out.add(FeatureRuleProfile.DAMAGE);
        }
        if (f.getSaveAbility() != null && !f.getSaveAbility().isBlank()) {
            out.add(FeatureRuleProfile.SAVE_CHECK_ATTACK);
        }
        if ((f.getHealingDice() != null && !f.getHealingDice().isBlank()) || f.getHealingFlat() != null) {
            out.add(FeatureRuleProfile.HEALING);
        }
        return out;
    }

    private int countFormulas(ClassFeature f, List<FeatureRuleProfile> planned) {
        int n = 0;
        if (planned.contains(FeatureRuleProfile.DAMAGE) && f.getDamageDice() != null) {
            n++;
        }
        if (planned.contains(FeatureRuleProfile.HEALING)) {
            n++;
        }
        return n;
    }

    /** Create the feature_rule (+ revision) and any specialized row/formula; returns formulas created. */
    private int createRuleFor(ClassFeature feature, FeatureRuleProfile profile, int sort) {
        FeatureRule rule = ruleRepository.save(FeatureRule.builder()
                .ownerType(OWNER).ownerId(feature.getId())
                .ruleType(profile.getCode())
                .enabled(true)
                .reviewStatus(FeatureReviewStatus.NEEDS_REVIEW.getCode())
                .source(FeatureRuleSource.PARSER.getCode())
                .confidence(0.5)
                .sortOrder(sort)
                .notes("Автоматически извлечено из class_feature")
                .build());
        revisionService.createInitialDraft(rule, "backfill");

        int formulas = 0;
        if (profile == FeatureRuleProfile.DAMAGE && feature.getDamageDice() != null) {
            FeatureFormula formula = formulaRepository.save(FeatureFormula.builder()
                    .expression("dice(\"" + feature.getDamageDice() + "\")")
                    .expressionType("dice").resultType("dice")
                    .validationStatus("unknown")
                    .build());
            formulas++;
            damageRuleRepository.save(FeatureDamageRule.builder()
                    .featureRuleId(rule.getId())
                    .diceFormulaId(formula.getId())
                    .requiresAttackHit(Boolean.TRUE.equals(feature.getAttackRoll()))
                    .requiresSave(feature.getSaveAbility() != null)
                    .build());
        } else if (profile == FeatureRuleProfile.HEALING) {
            String expr = feature.getHealingDice() != null && !feature.getHealingDice().isBlank()
                    ? "dice(\"" + feature.getHealingDice() + "\")"
                    : String.valueOf(feature.getHealingFlat() != null ? feature.getHealingFlat() : 0);
            String resultType = feature.getHealingDice() != null && !feature.getHealingDice().isBlank()
                    ? "dice" : "integer";
            FeatureFormula formula = formulaRepository.save(FeatureFormula.builder()
                    .expression(expr).expressionType(resultType).resultType(resultType)
                    .validationStatus("unknown")
                    .build());
            formulas++;
            healingRuleRepository.save(FeatureHealingRule.builder()
                    .featureRuleId(rule.getId())
                    .amountFormulaId(formula.getId())
                    .build());
        }
        return formulas;
    }
}
