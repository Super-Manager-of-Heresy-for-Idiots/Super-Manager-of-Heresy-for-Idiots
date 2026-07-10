package com.dnd.app.service;

import com.dnd.app.domain.Background;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.domain.featurerule.FeatureProficiencyGrant;
import com.dnd.app.domain.featurerule.FeatureProficiencyType;
import com.dnd.app.domain.featurerule.FeatureReviewStatus;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.domain.featurerule.FeatureRuleProfile;
import com.dnd.app.domain.featurerule.FeatureRuleSource;
import com.dnd.app.domain.featurerule.GrantTiming;
import com.dnd.app.repository.BackgroundRepository;
import com.dnd.app.repository.FeatureProficiencyGrantRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Класс BackgroundProficiencyBackfillService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackgroundProficiencyBackfillService {

    private static final String OWNER = FeatureRuleOwnerType.BACKGROUND.getCode();

    private final BackgroundRepository backgroundRepository;
    private final FeatureRuleRepository ruleRepository;
    private final FeatureProficiencyGrantRepository proficiencyGrantRepository;
    private final FeatureRuleRevisionService revisionService;

    /**
     * Выполняет обратное заполнение операции "backfill" в рамках бизнес-логики домена.
     * @param apply признак применения изменений вместо пробного расчета
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public int backfill(boolean apply) {
        int rulesCreated = 0;
        for (Background bg : backgroundRepository.findAll()) {
            boolean hasRules = !ruleRepository
                    .findByOwnerTypeAndOwnerIdOrderBySortOrderAscCreatedAtAsc(OWNER, bg.getId()).isEmpty();
            if (hasRules) {
                continue; // idempotent — already backfilled
            }
            List<ContentSkill> skills = bg.getSkillProficiencies();
            if (skills == null || skills.isEmpty()) {
                continue;
            }
            if (!apply) {
                rulesCreated++;
                continue;
            }

            FeatureRule rule = ruleRepository.save(FeatureRule.builder()
                    .ownerType(OWNER).ownerId(bg.getId())
                    .ruleType(FeatureRuleProfile.STATIC_GRANT.getCode())
                    .enabled(true)
                    .reviewStatus(FeatureReviewStatus.NEEDS_REVIEW.getCode())
                    .source(FeatureRuleSource.SEED.getCode())
                    .confidence(1.0)
                    .sortOrder(0)
                    .notes("Владения навыками из предыстории")
                    .build());
            revisionService.createInitialDraft(rule, "background-backfill");

            for (ContentSkill skill : skills) {
                proficiencyGrantRepository.save(FeatureProficiencyGrant.builder()
                        .featureRuleId(rule.getId())
                        .proficiencyType(FeatureProficiencyType.SKILL.getCode())
                        .targetId(skill.getId())
                        .grantTiming(GrantTiming.ALWAYS.getCode())
                        .expertise(false)
                        .build());
            }

            revisionService.approveCurrent(rule.getId(),
                    "Auto-approved deterministic background static grant", "background-backfill");
            rulesCreated++;
        }
        if (apply && rulesCreated > 0) {
            log.info("Background proficiency backfill created {} rule(s)", rulesCreated);
        }
        return rulesCreated;
    }
}
