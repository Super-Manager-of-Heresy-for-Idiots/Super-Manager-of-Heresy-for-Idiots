package com.dnd.app.mapper;

import com.dnd.app.domain.StatType;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.content.ClassLevelRewardGrant;
import com.dnd.app.domain.content.ClassLevelRewardGroup;
import com.dnd.app.domain.content.ClassLevelRewardOption;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.domain.content.ContentSubclass;
import com.dnd.app.dto.content.ClassFeatureSummaryDto;
import com.dnd.app.dto.content.ContentClassDetailResponse;
import com.dnd.app.dto.content.ContentLabelDto;
import com.dnd.app.dto.content.RewardGrantDto;
import com.dnd.app.dto.content.RewardGroupDto;
import com.dnd.app.dto.content.RewardOptionDto;
import com.dnd.app.dto.content.SpellcastingDto;
import com.dnd.app.dto.content.grant.AbilityScoreGrantPayload;
import com.dnd.app.dto.content.grant.CustomTextGrantPayload;
import com.dnd.app.dto.content.grant.FeatGrantPayload;
import com.dnd.app.dto.content.grant.FeatureGrantPayload;
import com.dnd.app.dto.content.grant.GrantPayload;
import com.dnd.app.dto.content.grant.GrantType;
import com.dnd.app.dto.content.grant.NumericModifierGrantPayload;
import com.dnd.app.dto.content.grant.SkillProficiencyGrantPayload;
import com.dnd.app.dto.content.grant.SpellGrantPayload;
import com.dnd.app.dto.content.grant.SubclassGrantPayload;
import com.dnd.app.repository.ClassFeatureRepository;
import com.dnd.app.repository.ClassLevelRewardGrantAbilityScoreRepository;
import com.dnd.app.repository.ClassLevelRewardGrantCustomTextRepository;
import com.dnd.app.repository.ClassLevelRewardGrantFeatRepository;
import com.dnd.app.repository.ClassLevelRewardGrantFeatureRepository;
import com.dnd.app.repository.ClassLevelRewardGrantNumericModifierRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSkillProficiencyRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSpellRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSubclassRepository;
import com.dnd.app.repository.ClassLevelRewardGroupRepository;
import com.dnd.app.util.Localization;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Maps the new-content class graph ({@link ContentCharacterClass} + features +
 * reward groups/options/grants + typed grant tables) to the canonical
 * {@link ContentClassDetailResponse} read model.
 *
 * <p>Must be invoked inside a read-only transaction: lazy associations (typed grant
 * detail rows and their option collections) are resolved on demand. N+1 access here
 * is acceptable for Phase 3 and addressed in Phase 11.</p>
 */
@Component
@RequiredArgsConstructor
public class ContentClassMapper {

    private final ClassFeatureRepository featureRepo;
    private final ClassLevelRewardGroupRepository groupRepo;
    private final ClassLevelRewardGrantFeatureRepository grantFeatureRepo;
    private final ClassLevelRewardGrantSubclassRepository grantSubclassRepo;
    private final ClassLevelRewardGrantFeatRepository grantFeatRepo;
    private final ClassLevelRewardGrantSpellRepository grantSpellRepo;
    private final ClassLevelRewardGrantSkillProficiencyRepository grantSkillRepo;
    private final ClassLevelRewardGrantAbilityScoreRepository grantAbilityRepo;
    private final ClassLevelRewardGrantNumericModifierRepository grantNumericRepo;
    private final ClassLevelRewardGrantCustomTextRepository grantCustomRepo;

    public ContentClassDetailResponse toDetail(ContentCharacterClass c, String lang) {
        return ContentClassDetailResponse.builder()
                .id(c.getId())
                .slug(c.getSlug())
                .name(Localization.pick(lang, c.getNameRu(), c.getNameEn(), fallback(c.getNameEn(), c.getNameRu())))
                .nameRu(c.getNameRu())
                .nameEn(c.getNameEn())
                .subtitle(c.getSubtitle())
                // description is not modeled on the class entity in the new schema
                .packageId(c.getHomebrew() != null ? c.getHomebrew().getId() : null)
                .hitDie(c.getHitDie())
                .primaryAbilities(c.getPrimaryAbilities().stream()
                        .map(s -> abilityLabel(s, lang))
                        .sorted(Comparator.comparing(ContentLabelDto::getSlug, Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList())
                .savingThrows(c.getSavingThrows().stream()
                        .map(s -> abilityLabel(s, lang))
                        .sorted(Comparator.comparing(ContentLabelDto::getSlug, Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList())
                .skillChoiceCount(c.getSkillChoiceCount())
                .skillChoiceAny(c.getSkillChoiceAny())
                .skillOptions(c.getSkillOptions().stream()
                        .map(s -> skillLabel(s, lang))
                        .sorted(Comparator.comparing(ContentLabelDto::getSlug, Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList())
                .armorProficiencyText(c.getArmorProficiencyText())
                .weaponProficiencyText(c.getWeaponProficiencyText())
                .toolProficiencyText(c.getToolProficiencyText())
                .spellcasting(mapSpellcasting(c, lang))
                .features(mapFeatures(c.getId()))
                .rewardGroups(mapRewardGroups(c.getId(), lang))
                .build();
    }

    // --- spellcasting ---

    private SpellcastingDto mapSpellcasting(ContentCharacterClass c, String lang) {
        if (!Boolean.TRUE.equals(c.getSpellcaster())) {
            return null;
        }
        StatType ability = c.getSpellcastingAbility();
        return SpellcastingDto.builder()
                .casterProgression(Boolean.TRUE.equals(c.getHalfCaster()) ? "HALF" : "FULL")
                .spellcastingAbilityId(ability != null ? ability.getId() : null)
                .spellcastingAbility(ability != null ? abilityLabel(ability, lang) : null)
                .hasCantrips(c.getHasCantrips())
                .halfCaster(c.getHalfCaster())
                .build();
    }

    // --- features ---

    private List<ClassFeatureSummaryDto> mapFeatures(UUID classId) {
        return featureRepo.findAllByCharacterClassIdOrderByLevelAscSortOrderAsc(classId).stream()
                .map(this::mapFeature)
                .toList();
    }

    private ClassFeatureSummaryDto mapFeature(ClassFeature f) {
        return ClassFeatureSummaryDto.builder()
                .id(f.getId())
                .slug(f.getSlug())
                .classId(f.getCharacterClass() != null ? f.getCharacterClass().getId() : null)
                .subclassId(f.getSubclass() != null ? f.getSubclass().getId() : null)
                .level(f.getLevel())
                .sortOrder(f.getSortOrder())
                .title(f.getTitle())
                .description(f.getDescription())
                .build();
    }

    // --- reward groups / options / grants ---

    private List<RewardGroupDto> mapRewardGroups(UUID classId, String lang) {
        return groupRepo.findAllByCharacterClassIdOrderByClassLevelAscSortOrderAsc(classId).stream()
                .map(g -> mapGroup(g, lang))
                .toList();
    }

    private RewardGroupDto mapGroup(ClassLevelRewardGroup g, String lang) {
        return RewardGroupDto.builder()
                .id(g.getId())
                .classId(g.getCharacterClass() != null ? g.getCharacterClass().getId() : null)
                .classFeatureId(g.getClassFeature() != null ? g.getClassFeature().getId() : null)
                .classLevel(g.getClassLevel())
                .groupKind(g.getGroupKind())
                .prompt(Localization.pick(lang, g.getPromptRu(), g.getPromptEn(), g.getPromptEn()))
                .description(g.getDescription())
                .chooseMin(g.getChooseMin())
                .chooseMax(g.getChooseMax())
                .repeatable(g.getRepeatable())
                .sortOrder(g.getSortOrder())
                .options(g.getOptions().stream().map(o -> mapOption(o, lang)).toList())
                .grants(g.getGrants().stream().map(gr -> mapGrant(gr, lang)).toList())
                .build();
    }

    private RewardOptionDto mapOption(ClassLevelRewardOption o, String lang) {
        return RewardOptionDto.builder()
                .id(o.getId())
                .optionKey(o.getOptionKey())
                .label(Localization.pick(lang, o.getLabelRu(), o.getLabelEn(), o.getLabelEn()))
                .labelRu(o.getLabelRu())
                .labelEn(o.getLabelEn())
                .description(o.getDescription())
                .recommended(o.getRecommended())
                .sortOrder(o.getSortOrder())
                .grants(o.getGrants().stream().map(gr -> mapGrant(gr, lang)).toList())
                .build();
    }

    private RewardGrantDto mapGrant(ClassLevelRewardGrant grant, String lang) {
        return RewardGrantDto.builder()
                .id(grant.getId())
                .grantType(grant.getGrantType())
                .label(Localization.pick(lang, grant.getLabelRu(), grant.getLabelEn(), grant.getLabelEn()))
                .labelRu(grant.getLabelRu())
                .labelEn(grant.getLabelEn())
                .description(grant.getDescription())
                .sortOrder(grant.getSortOrder())
                .payload(mapPayload(grant, lang))
                .build();
    }

    private GrantPayload mapPayload(ClassLevelRewardGrant grant, String lang) {
        UUID id = grant.getId();
        GrantType type = GrantType.fromTextOrCustom(grant.getGrantType());
        return switch (type) {
            case FEATURE -> grantFeatureRepo.findById(id)
                    .map(d -> (GrantPayload) FeatureGrantPayload.builder()
                            .featureId(d.getClassFeature() != null ? d.getClassFeature().getId() : null)
                            .build())
                    .orElseGet(() -> customFallback(grant));
            case SUBCLASS -> grantSubclassRepo.findById(id)
                    .map(d -> (GrantPayload) SubclassGrantPayload.builder()
                            .subclassId(d.getSubclass() != null ? d.getSubclass().getId() : null)
                            .subclass(d.getSubclass() != null ? subclassLabel(d.getSubclass(), lang) : null)
                            .build())
                    .orElseGet(() -> customFallback(grant));
            case FEAT -> grantFeatRepo.findById(id)
                    .map(d -> (GrantPayload) FeatGrantPayload.builder()
                            .mode("FIXED")
                            .featId(d.getFeat() != null ? d.getFeat().getId() : null)
                            .build())
                    .orElseGet(() -> FeatGrantPayload.builder().mode("ANY").chooseCount(1).build());
            case SPELL -> grantSpellRepo.findById(id)
                    .map(d -> {
                        if (d.getSpell() != null) {
                            return (GrantPayload) SpellGrantPayload.builder()
                                    .mode("FIXED")
                                    .fixedSpellIds(List.of(d.getSpell().getId()))
                                    .build();
                        }
                        return (GrantPayload) SpellGrantPayload.builder()
                                .mode("CHOICE")
                                .spellLevel(d.getSpellLevel())
                                .schoolIds(d.getSchool() != null ? List.of(d.getSchool().getId()) : null)
                                .chooseCount(d.getChooseCount())
                                .build();
                    })
                    .orElseGet(() -> customFallback(grant));
            case SKILL_PROFICIENCY -> grantSkillRepo.findById(id)
                    .map(d -> {
                        if (Boolean.TRUE.equals(d.getAnySkill())) {
                            return (GrantPayload) SkillProficiencyGrantPayload.builder()
                                    .mode("ANY").chooseCount(d.getChooseCount()).build();
                        }
                        if (d.getSkillOptions() != null && !d.getSkillOptions().isEmpty()) {
                            return (GrantPayload) SkillProficiencyGrantPayload.builder()
                                    .mode("CHOICE")
                                    .chooseCount(d.getChooseCount())
                                    .skillOptionIds(d.getSkillOptions().stream().map(ContentSkill::getId).toList())
                                    .build();
                        }
                        return (GrantPayload) SkillProficiencyGrantPayload.builder()
                                .mode("FIXED")
                                .skillIds(d.getSkill() != null ? List.of(d.getSkill().getId()) : List.of())
                                .build();
                    })
                    .orElseGet(() -> customFallback(grant));
            case ABILITY_SCORE -> grantAbilityRepo.findById(id)
                    .map(d -> (GrantPayload) AbilityScoreGrantPayload.builder()
                            .chooseCount(d.getChooseCount())
                            .bonusPerChoice(d.getBonusPerChoice())
                            .totalBonus(d.getTotalBonus())
                            .maxPerAbility(d.getMaxPerAbility())
                            .maxScore(d.getMaxScore())
                            .abilityOptionIds(abilityOptionIds(d))
                            .build())
                    .orElseGet(() -> customFallback(grant));
            case NUMERIC_MODIFIER -> grantNumericRepo.findById(id)
                    .map(d -> (GrantPayload) NumericModifierGrantPayload.builder()
                            .modifierKey(d.getModifierKey())
                            .amount(d.getAmount() != null ? d.getAmount().intValue() : null)
                            .unitText(d.getUnitText())
                            .durationText(d.getDurationText())
                            .build())
                    .orElseGet(() -> customFallback(grant));
            case CUSTOM_TEXT -> grantCustomRepo.findById(id)
                    .map(d -> (GrantPayload) CustomTextGrantPayload.builder()
                            .title(Localization.pick(lang, d.getTitleRu(), d.getTitleEn(), d.getTitleEn()))
                            .body(d.getBody())
                            .userEditable(d.getUserEditable())
                            .build())
                    .orElseGet(() -> customFallback(grant));
        };
    }

    /** Fallback for known-typed grants missing their detail row, or unknown grant types. */
    private GrantPayload customFallback(ClassLevelRewardGrant grant) {
        return CustomTextGrantPayload.builder()
                .title(grant.getLabelEn() != null ? grant.getLabelEn() : grant.getLabelRu())
                .body(grant.getDescription())
                .userEditable(true)
                .build();
    }

    private List<UUID> abilityOptionIds(com.dnd.app.domain.content.ClassLevelRewardGrantAbilityScore d) {
        if (d.getAbilityOptions() != null && !d.getAbilityOptions().isEmpty()) {
            return d.getAbilityOptions().stream().map(StatType::getId).toList();
        }
        if (d.getAbilityScore() != null) {
            return List.of(d.getAbilityScore().getId());
        }
        return List.of();
    }

    // --- label helpers ---

    private ContentLabelDto abilityLabel(StatType s, String lang) {
        return ContentLabelDto.builder()
                .id(s.getId())
                .slug(s.getSlug())
                .name(Localization.pick(lang, s.getNameRu(), s.getNameEn(), fallback(s.getNameEn(), s.getNameRu())))
                .nameRu(s.getNameRu())
                .nameEn(s.getNameEn())
                .build();
    }

    private ContentLabelDto skillLabel(ContentSkill s, String lang) {
        return ContentLabelDto.builder()
                .id(s.getId())
                .slug(s.getSlug())
                .name(Localization.pick(lang, s.getNameRu(), s.getNameEn(), fallback(s.getNameEn(), s.getNameRu())))
                .nameRu(s.getNameRu())
                .nameEn(s.getNameEn())
                .build();
    }

    private ContentLabelDto subclassLabel(ContentSubclass s, String lang) {
        return ContentLabelDto.builder()
                .id(s.getId())
                .slug(s.getSlug())
                .name(Localization.pick(lang, s.getNameRu(), s.getNameEn(), fallback(s.getNameEn(), s.getNameRu())))
                .nameRu(s.getNameRu())
                .nameEn(s.getNameEn())
                .build();
    }

    private String fallback(String preferred, String other) {
        return preferred != null ? preferred : other;
    }
}
