package com.dnd.app.service;

import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.featurerule.FeatureReviewStatus;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.domain.featurerule.FeatureRuleOwnerType;
import com.dnd.app.repository.CharacterClassLevelRepository;
import com.dnd.app.repository.ClassFeatureRepository;
import com.dnd.app.repository.FeatureRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Resolves which base-class features a character has, and their approved+enabled feature rules. */
@Service
@RequiredArgsConstructor
public class CharacterFeatureResolver {

    private static final String OWNER = FeatureRuleOwnerType.CLASS_FEATURE.getCode();
    private static final String APPROVED = FeatureReviewStatus.APPROVED.getCode();

    private final CharacterClassLevelRepository classLevelRepository;
    private final ClassFeatureRepository classFeatureRepository;
    private final FeatureRuleRepository ruleRepository;

    /** All base-class (subclass == null) features the character has, i.e. feature level ≤ its class level. */
    @Transactional(readOnly = true)
    public List<ClassFeature> knownBaseClassFeatures(UUID characterId) {
        List<ClassFeature> out = new ArrayList<>();
        for (CharacterClassLevel ccl : classLevelRepository.findAllByCharacterId(characterId)) {
            int classLevel = ccl.getClassLevel() != null ? ccl.getClassLevel() : 0;
            classFeatureRepository.findAllByCharacterClassIdOrderByLevelAscSortOrderAsc(ccl.getClassId()).stream()
                    .filter(f -> f.getSubclass() == null && f.getLevel() != null && f.getLevel() <= classLevel)
                    .forEach(out::add);
        }
        return out;
    }

    /** Approved, enabled, runtime-eligible rules (with an approved revision) for the given features. */
    @Transactional(readOnly = true)
    public List<FeatureRule> approvedEnabledRules(Collection<UUID> featureIds) {
        if (featureIds.isEmpty()) {
            return List.of();
        }
        return ruleRepository.findByOwnerTypeAndOwnerIdIn(OWNER, featureIds).stream()
                .filter(FeatureRule::isEnabled)
                .filter(r -> APPROVED.equals(r.getReviewStatus()) && r.getApprovedRevisionId() != null)
                .toList();
    }
}
