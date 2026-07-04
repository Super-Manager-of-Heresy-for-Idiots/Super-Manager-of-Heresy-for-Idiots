package com.dnd.app.service;

import com.dnd.app.config.FeatureRulesProperties;
import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.CharacterStat;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.featurerule.ActiveEffectStatus;
import com.dnd.app.domain.featurerule.FeatureChoiceGroup;
import com.dnd.app.domain.featurerule.FeatureRule;
import com.dnd.app.dto.content.CharacterClassFeatureResponse;
import com.dnd.app.dto.featurerule.CapabilityProfileResponse;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CharacterFeatureChoiceRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.FeatureActiveEffectRepository;
import com.dnd.app.repository.FeatureAllowedMonsterFilterRepository;
import com.dnd.app.repository.FeatureChoiceGroupRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.util.AbilityScores;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builds a character's {@link CapabilityProfileResponse} — the single source of truth the frontend uses to
 * render class-aware panels/tabs (see {@code docs/FEATURE_RULES_FRONTEND_REWORK_PLAN.md} §0).
 *
 * <p>Spellcasting is derived from class content ({@code character_class}) and is always populated, so the
 * frontend can gate the spells tab by {@code spellcasting.caster} regardless of the runtime flags. All
 * feature-rules presence flags are only computed when the matching {@code app.feature-rules.*} subsystem is
 * active, so the profile never advertises something the backend will not actually serve.</p>
 */
@Service
@RequiredArgsConstructor
public class CharacterCapabilityProfileService {

    private final PlayerCharacterRepository characterRepository;
    private final ContentCharacterClassRepository classRepository;
    private final CharacterFeatureResolver featureResolver;
    private final FeatureRulesProperties featureRules;

    private final FeatureResourceService featureResourceService;
    private final FeatureActionService featureActionService;
    private final FeatureCompanionService featureCompanionService;
    private final FeatureSpellGrantService featureSpellGrantService;
    private final CharacterFormService characterFormService;
    private final PendingGameplayPromptService pendingGameplayPromptService;

    private final FeatureActiveEffectRepository effectRepository;
    private final FeatureAllowedMonsterFilterRepository allowedMonsterFilterRepository;
    private final FeatureChoiceGroupRepository choiceGroupRepository;
    private final CharacterFeatureChoiceRepository choiceRepository;

    @Transactional(readOnly = true)
    public CapabilityProfileResponse build(UUID characterId) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));

        int totalLevel = character.getTotalLevel() != null ? character.getTotalLevel() : 1;
        int pb = proficiencyBonus(totalLevel);

        // Ability score value keyed by ability id, for spell save DC / attack.
        Map<UUID, Integer> abilityScores = new HashMap<>();
        for (CharacterStat s : character.getStats()) {
            if (s.getStatType() != null && s.getValue() != null && !Boolean.TRUE.equals(s.getDeprecated())) {
                abilityScores.put(s.getStatType().getId(), s.getValue());
            }
        }

        // Per-class breakdown + spellcasting, from class content (always available, flag-independent).
        List<CapabilityProfileResponse.ClassCapability> classes = new ArrayList<>();
        ContentCharacterClass primaryCaster = null;
        int primaryCasterLevel = -1;
        int casterClassCount = 0;
        for (CharacterClassLevel ccl : character.getClassLevels()) {
            ContentCharacterClass cc = ccl.getClassId() != null
                    ? classRepository.findById(ccl.getClassId()).orElse(null) : null;
            boolean caster = cc != null && Boolean.TRUE.equals(cc.getSpellcaster());
            String type = caster ? (Boolean.TRUE.equals(cc.getHalfCaster()) ? "HALF" : "FULL") : "NONE";
            classes.add(CapabilityProfileResponse.ClassCapability.builder()
                    .classId(ccl.getClassId())
                    .classNameRu(cc != null ? cc.getNameRu() : null)
                    .classNameEn(cc != null ? cc.getNameEn() : null)
                    .classLevel(ccl.getClassLevel())
                    .caster(caster)
                    .casterType(type)
                    .build());
            if (caster) {
                casterClassCount++;
                int lvl = ccl.getClassLevel() != null ? ccl.getClassLevel() : 0;
                if (lvl > primaryCasterLevel) {
                    primaryCasterLevel = lvl;
                    primaryCaster = cc;
                }
            }
        }

        CapabilityProfileResponse.CapabilityProfileResponseBuilder out = CapabilityProfileResponse.builder()
                .characterId(characterId)
                .totalLevel(totalLevel)
                .proficiencyBonus(pb)
                .runtimeEnabled(featureRules.isRuntimeEnabled())
                .spellcasting(buildSpellcasting(primaryCaster, casterClassCount, pb, abilityScores))
                .classes(classes);

        // Approved+enabled rule ids for this character (only needed when the runtime is on).
        List<UUID> ruleIds = List.of();
        if (featureRules.isRuntimeEnabled()) {
            List<ClassFeature> feats = featureResolver.knownBaseClassFeatures(characterId);
            ruleIds = featureResolver.approvedEnabledRules(feats.stream().map(ClassFeature::getId).toList())
                    .stream().map(FeatureRule::getId).toList();
        }

        if (featureRules.resourcesActive()) {
            out.hasFeatureResources(!featureResourceService.list(characterId).isEmpty());
        }
        if (featureRules.actionsActive()) {
            out.hasFeatureActions(!featureActionService.listAvailableActions(character).isEmpty());
        }
        if (featureRules.effectsActive()) {
            out.hasActiveEffects(!effectRepository
                    .findByCharacterIdAndStatus(characterId, ActiveEffectStatus.ACTIVE.getCode()).isEmpty());
        }
        if (featureRules.spellsActive()) {
            out.hasFeatureSpellGrants(!featureSpellGrantService.listGrantedSpells(character).isEmpty());
        }
        if (featureRules.formsActive()) {
            out.hasCompanions(!featureCompanionService.listCompanions(character).isEmpty());
            out.wildShape(buildWildShape(characterId, ruleIds));
        }
        if (featureRules.triggersActive()) {
            out.pendingPrompts(pendingGameplayPromptService.listPending(characterId).size());
        }
        if (featureRules.isRuntimeEnabled()) {
            out.pendingChoices(countPendingChoices(characterId, ruleIds));
        }

        return out.build();
    }

    /**
     * The character's structured class features (base-class, feature level ≤ its class level) for the folio
     * "Features" tab — the real class abilities (Reckless Attack, Wild Shape, …), not a prose blob. Available
     * for every class independent of the runtime flags. (Subclass features are a follow-up.)
     */
    @Transactional(readOnly = true)
    public List<CharacterClassFeatureResponse> listClassFeatures(UUID characterId) {
        return featureResolver.knownBaseClassFeatures(characterId).stream()
                .map(f -> CharacterClassFeatureResponse.builder()
                        .id(f.getId())
                        .classId(f.getCharacterClass() != null ? f.getCharacterClass().getId() : null)
                        .className(f.getCharacterClass() != null ? f.getCharacterClass().getNameRu() : null)
                        .level(f.getLevel())
                        .title(f.getTitle())
                        .description(f.getDescription())
                        .activationType(f.getActivationType())
                        .build())
                .toList();
    }

    private CapabilityProfileResponse.SpellcastingCapability buildSpellcasting(
            ContentCharacterClass primaryCaster, int casterClassCount, int pb, Map<UUID, Integer> abilityScores) {
        if (primaryCaster == null) {
            return CapabilityProfileResponse.SpellcastingCapability.builder()
                    .caster(false).casterType("NONE").build();
        }
        String casterType;
        if (casterClassCount > 1) {
            casterType = "MULTI";
        } else if (primaryCaster.getCasterType() != null && !primaryCaster.getCasterType().isBlank()) {
            casterType = primaryCaster.getCasterType();
        } else {
            casterType = Boolean.TRUE.equals(primaryCaster.getHalfCaster()) ? "HALF" : "FULL";
        }

        UUID abilityId = null;
        String ru = null;
        String en = null;
        Integer dc = null;
        Integer atk = null;
        StatType ability = primaryCaster.getSpellcastingAbility();
        if (ability != null) {
            abilityId = ability.getId();
            ru = ability.getNameRu();
            en = ability.getNameEn();
            Integer score = abilityScores.get(ability.getId());
            if (score != null) {
                int mod = AbilityScores.modifier(score);
                dc = 8 + pb + mod;
                atk = pb + mod;
            }
        }
        return CapabilityProfileResponse.SpellcastingCapability.builder()
                .caster(true)
                .casterType(casterType)
                .hasCantrips(Boolean.TRUE.equals(primaryCaster.getHasCantrips()))
                .abilityId(abilityId)
                .abilityNameRu(ru)
                .abilityNameEn(en)
                .spellSaveDc(dc)
                .spellAttackBonus(atk)
                .build();
    }

    /** Wild-shape/forms capability; null when the class cannot transform and has no forms. */
    private CapabilityProfileResponse.WildShapeCapability buildWildShape(UUID characterId, List<UUID> ruleIds) {
        boolean canWildShape = !ruleIds.isEmpty()
                && !allowedMonsterFilterRepository.findByFeatureRuleIdIn(ruleIds).isEmpty();
        int knownFormCount = characterFormService.listKnownForms(characterId).size();
        boolean activeTransformation = characterFormService.currentTransformation(characterId) != null;
        if (!canWildShape && knownFormCount == 0 && !activeTransformation) {
            return null;
        }
        return CapabilityProfileResponse.WildShapeCapability.builder()
                .canWildShape(canWildShape)
                .knownFormCount(knownFormCount)
                .activeTransformation(activeTransformation)
                .build();
    }

    private int countPendingChoices(UUID characterId, List<UUID> ruleIds) {
        if (ruleIds.isEmpty()) {
            return 0;
        }
        int pending = 0;
        for (FeatureChoiceGroup g : choiceGroupRepository.findByFeatureRuleIdIn(ruleIds)) {
            long made = choiceRepository.countByCharacterIdAndChoiceGroupId(characterId, g.getId());
            int min = g.getMinChoices() != null ? g.getMinChoices() : 0;
            if (made < min) {
                pending++;
            }
        }
        return pending;
    }

    /** D&D 5e proficiency bonus from total character level: 2 at levels 1–4, +1 every 4 levels. */
    private static int proficiencyBonus(int totalLevel) {
        return 2 + (Math.max(1, totalLevel) - 1) / 4;
    }
}
