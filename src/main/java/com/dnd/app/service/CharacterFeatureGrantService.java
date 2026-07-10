package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.CharacterSkillProficiency;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.domain.enums.SkillProficiencyLevel;
import com.dnd.app.domain.enums.SkillProficiencySource;
import com.dnd.app.domain.featurerule.FeatureProficiencyGrant;
import com.dnd.app.domain.featurerule.FeatureProficiencyType;
import com.dnd.app.domain.featurerule.FeatureReviewStatus;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.domain.featurerule.GrantTiming;
import com.dnd.app.repository.CharacterSkillProficiencyRepository;
import com.dnd.app.repository.ClassFeatureRepository;
import com.dnd.app.repository.ContentSkillRepository;
import com.dnd.app.repository.FeatureProficiencyGrantRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Класс CharacterFeatureGrantService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterFeatureGrantService {

    private static final String OWNER = FeatureRuleOwnerType.CLASS_FEATURE.getCode();
    private static final String APPROVED = FeatureReviewStatus.APPROVED.getCode();

    private final FeatureRulesProperties flags;
    private final ClassFeatureRepository classFeatureRepository;
    private final FeatureRuleRepository ruleRepository;
    private final FeatureProficiencyGrantRepository proficiencyGrantRepository;
    private final CharacterSkillProficiencyRepository skillProficiencyRepository;
    private final ContentSkillRepository contentSkillRepository;
    private final FeatureResourceService featureResourceService;

    /**
     * Выполняет операции "apply for class level" в рамках бизнес-логики домена.
     * @param character входящее значение character, используемое бизнес-сценарием
     * @param classId идентификатор class, используемый для выбора нужного бизнес-объекта
     * @param classLevel входящее значение class level, используемое бизнес-сценарием
     */
    @Transactional
    public void applyForClassLevel(PlayerCharacter character, UUID classId, int classLevel) {
        if (!flags.isRuntimeEnabled()) {
            return; // hard no-op: existing flow unaffected
        }
        try {
            List<ClassFeature> features = classFeatureRepository
                    .findAllByCharacterClassIdOrderByLevelAscSortOrderAsc(classId).stream()
                    .filter(f -> f.getSubclass() == null && Integer.valueOf(classLevel).equals(f.getLevel()))
                    .toList();
            if (features.isEmpty()) {
                return;
            }
            List<UUID> featureIds = features.stream().map(ClassFeature::getId).toList();

            List<FeatureRule> approvedRules = ruleRepository.findByOwnerTypeAndOwnerIdIn(OWNER, featureIds).stream()
                    .filter(FeatureRule::isEnabled)
                    .filter(r -> APPROVED.equals(r.getReviewStatus()) && r.getApprovedRevisionId() != null)
                    .toList();
            if (approvedRules.isEmpty()) {
                return;
            }
            List<UUID> ruleIds = approvedRules.stream().map(FeatureRule::getId).toList();

            List<FeatureProficiencyGrant> grants = proficiencyGrantRepository.findByFeatureRuleIdIn(ruleIds).stream()
                    .filter(g -> GrantTiming.LEVEL_UP.getCode().equals(g.getGrantTiming()))
                    .filter(g -> g.getTargetId() != null) // only concrete grants auto-apply; choices go through the choice flow
                    .filter(g -> FeatureProficiencyType.SKILL.getCode().equals(g.getProficiencyType()))
                    .toList();

            int applied = 0;
            for (FeatureProficiencyGrant grant : grants) {
                if (applySkill(character, grant.getTargetId(), grant.isExpertise(), SkillProficiencySource.FEATURE)) {
                    applied++;
                }
            }
            if (applied > 0) {
                log.info("Applied {} feature-rule skill grant(s) to character {} at class {} level {}",
                        applied, character.getId(), classId, classLevel);
            }

            // Stage 5: create resource state for newly-gained features and refresh maxima (both gated internally).
            featureResourceService.ensureResourcesForRules(character, ruleIds);
            featureResourceService.recalcMax(character);
        } catch (RuntimeException e) {
            log.warn("Feature-rule grant application skipped for character {} (class {}, level {}): {}",
                    character.getId(), classId, classLevel, e.getMessage());
        }
    }

    /**
     * Выполняет операции "apply for background" в рамках бизнес-логики домена.
     * @param character входящее значение character, используемое бизнес-сценарием
     * @param backgroundId идентификатор background, используемый для выбора нужного бизнес-объекта
     */
    @Transactional
    public void applyForBackground(PlayerCharacter character, UUID backgroundId) {
        if (!flags.isRuntimeEnabled() || backgroundId == null) {
            return; // hard no-op: existing creation flow unaffected
        }
        try {
            List<FeatureRule> approvedRules = ruleRepository
                    .findByOwnerTypeAndOwnerIdIn(FeatureRuleOwnerType.BACKGROUND.getCode(), List.of(backgroundId)).stream()
                    .filter(FeatureRule::isEnabled)
                    .filter(r -> APPROVED.equals(r.getReviewStatus()) && r.getApprovedRevisionId() != null)
                    .toList();
            if (approvedRules.isEmpty()) {
                return;
            }
            List<UUID> ruleIds = approvedRules.stream().map(FeatureRule::getId).toList();

            List<FeatureProficiencyGrant> grants = proficiencyGrantRepository.findByFeatureRuleIdIn(ruleIds).stream()
                    .filter(g -> g.getTargetId() != null)
                    .filter(g -> FeatureProficiencyType.SKILL.getCode().equals(g.getProficiencyType()))
                    .toList();

            int applied = 0;
            for (FeatureProficiencyGrant grant : grants) {
                if (applySkill(character, grant.getTargetId(), grant.isExpertise(), SkillProficiencySource.BACKGROUND)) {
                    applied++;
                }
            }
            if (applied > 0) {
                log.info("Applied {} background skill grant(s) to character {} (background {})",
                        applied, character.getId(), backgroundId);
            }
        } catch (RuntimeException e) {
            log.warn("Background grant application skipped for character {} (background {}): {}",
                    character.getId(), backgroundId, e.getMessage());
        }
    }

    private boolean applySkill(PlayerCharacter character, UUID skillId, boolean expertise,
                               SkillProficiencySource source) {
        Optional<CharacterSkillProficiency> existing =
                skillProficiencyRepository.findByCharacterIdAndSkillId(character.getId(), skillId);

        if (expertise) {
            // Expertise upgrades an existing proficiency; if none exists we can't safely grant it here.
            if (existing.isPresent() && existing.get().getProficiencyLevel() != SkillProficiencyLevel.EXPERTISE) {
                existing.get().setProficiencyLevel(SkillProficiencyLevel.EXPERTISE);
                skillProficiencyRepository.save(existing.get());
                return true;
            }
            return false;
        }

        if (existing.isPresent()) {
            return false; // already proficient; don't duplicate
        }
        ContentSkill skill = contentSkillRepository.findById(skillId).orElse(null);
        if (skill == null) {
            return false; // invalid grant target
        }
        skillProficiencyRepository.save(CharacterSkillProficiency.builder()
                .character(character)
                .skill(skill)
                .source(source)
                .proficiencyLevel(SkillProficiencyLevel.PROFICIENT)
                .build());
        return true;
    }
}
