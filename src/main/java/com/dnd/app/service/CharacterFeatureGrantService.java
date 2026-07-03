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
 * Applies approved feature-rule static grants to a character when it gains class features (parallel,
 * flag-gated layer next to the existing ClassLevelReward system — see
 * docs/FEATURE_RULES_RUNTIME_ROADMAP.md §Stage 4 coexistence decision).
 *
 * <p>When {@code app.feature-rules.runtime-enabled} is off (default) this is a hard no-op, so the
 * existing level-up/creation flow is completely unaffected. Failures in this experimental layer are
 * logged and swallowed so they can never break the critical character path.</p>
 *
 * <p>Today only SKILL proficiencies/expertise are materializable (that is the only character-side
 * proficiency store that exists). Weapon/armor/tool/language grants are authored but not yet applied —
 * they need new character-side storage (tracked as a Stage 4 follow-up).</p>
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

    /** Apply feature-rule static grants for the base-class features gained at {@code classLevel}. */
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
                if (applySkill(character, grant.getTargetId(), grant.isExpertise())) {
                    applied++;
                }
            }
            if (applied > 0) {
                log.info("Applied {} feature-rule skill grant(s) to character {} at class {} level {}",
                        applied, character.getId(), classId, classLevel);
            }
        } catch (RuntimeException e) {
            log.warn("Feature-rule grant application skipped for character {} (class {}, level {}): {}",
                    character.getId(), classId, classLevel, e.getMessage());
        }
    }

    private boolean applySkill(PlayerCharacter character, UUID skillId, boolean expertise) {
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
                .source(SkillProficiencySource.FEATURE)
                .proficiencyLevel(SkillProficiencyLevel.PROFICIENT)
                .build());
        return true;
    }
}
