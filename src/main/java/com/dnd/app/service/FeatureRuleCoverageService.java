package com.dnd.app.service;

import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.featurerule.FeatureIssueSeverity;
import com.dnd.app.domain.featurerule.FeatureReviewStatus;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleIssue;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.dto.featurerule.FeatureRuleCoverageReport;
import com.dnd.app.repository.ClassFeatureRepository;
import com.dnd.app.repository.FeatureRuleIssueRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import com.dnd.app.util.Localization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Класс FeatureRuleCoverageService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class FeatureRuleCoverageService {

    private static final String OWNER = FeatureRuleOwnerType.CLASS_FEATURE.getCode();
    private static final String APPROVED = FeatureReviewStatus.APPROVED.getCode();
    private static final String NEEDS_REVIEW = FeatureReviewStatus.NEEDS_REVIEW.getCode();
    private static final String ERROR = FeatureIssueSeverity.ERROR.getCode();

    private final ClassFeatureRepository classFeatureRepository;
    private final FeatureRuleRepository ruleRepository;
    private final FeatureRuleIssueRepository issueRepository;

    /**
     * Выполняет операции "report" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public FeatureRuleCoverageReport report(String lang) {
        Map<UUID, ClassFeature> features = classFeatureRepository.findAll().stream()
                .filter(f -> f.getSubclass() == null)
                .collect(Collectors.toMap(ClassFeature::getId, f -> f));
        int runtimeFeatures = features.size();

        List<FeatureRule> rules = ruleRepository.findByOwnerType(OWNER);
        Map<UUID, List<FeatureRule>> byFeature = rules.stream()
                .filter(r -> features.containsKey(r.getOwnerId()))
                .collect(Collectors.groupingBy(FeatureRule::getOwnerId));

        int withRules = byFeature.size();
        int withApproved = (int) byFeature.values().stream()
                .filter(list -> list.stream().anyMatch(r -> APPROVED.equals(r.getReviewStatus())))
                .count();

        Set<UUID> errorFeatures = issueRepository.findByOwnerType(OWNER).stream()
                .filter(i -> !i.isResolved() && ERROR.equals(i.getSeverity()))
                .map(FeatureRuleIssue::getOwnerId)
                .filter(features::containsKey)
                .collect(Collectors.toSet());

        Map<String, Long> rulesByType = rules.stream()
                .collect(Collectors.groupingBy(FeatureRule::getRuleType, Collectors.counting()));
        Map<String, Long> rulesByStatus = rules.stream()
                .collect(Collectors.groupingBy(FeatureRule::getReviewStatus, Collectors.counting()));

        Map<String, Long> coverageByClass = byFeature.keySet().stream()
                .map(features::get)
                .collect(Collectors.groupingBy(f -> className(f, lang), Collectors.counting()));

        long approved = rules.stream().filter(r -> APPROVED.equals(r.getReviewStatus())).count();
        long needsReview = rules.stream().filter(r -> NEEDS_REVIEW.equals(r.getReviewStatus())).count();

        return FeatureRuleCoverageReport.builder()
                .runtimeFeatures(runtimeFeatures)
                .featuresWithRules(withRules)
                .featuresWithApprovedRules(withApproved)
                .featuresWithoutRules(runtimeFeatures - withRules)
                .featuresWithUnresolvedError(errorFeatures.size())
                .totalRules(rules.size())
                .approvedRules(approved)
                .needsReviewRules(needsReview)
                .rulesByType(rulesByType)
                .rulesByStatus(rulesByStatus)
                .coverageByClass(coverageByClass)
                .build();
    }

    private String className(ClassFeature f, String lang) {
        if (f.getCharacterClass() == null) {
            return "—";
        }
        String name = Localization.pick(lang,
                f.getCharacterClass().getNameRu(),
                f.getCharacterClass().getNameEn(),
                f.getCharacterClass().getNameRu());
        return name != null ? name : "—";
    }
}
