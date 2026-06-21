package com.dnd.app.mapper;

import com.dnd.app.domain.StatType;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.content.ClassLevelRewardGrant;
import com.dnd.app.domain.content.ClassLevelRewardGrantAbilityScore;
import com.dnd.app.domain.content.ClassLevelRewardGrantCustomText;
import com.dnd.app.domain.content.ClassLevelRewardGrantFeat;
import com.dnd.app.domain.content.ClassLevelRewardGrantFeature;
import com.dnd.app.domain.content.ClassLevelRewardGrantNumericModifier;
import com.dnd.app.domain.content.ClassLevelRewardGrantSkillProficiency;
import com.dnd.app.domain.content.ClassLevelRewardGrantSpell;
import com.dnd.app.domain.content.ClassLevelRewardGrantSubclass;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        List<ClassLevelRewardGroup> groups =
                groupRepo.findAllByCharacterClassIdOrderByClassLevelAscSortOrderAsc(classId);
        GrantDetailCache cache = buildCache(collectGrants(groups));
        return groups.stream().map(g -> mapGroup(g, lang, cache)).toList();
    }

    /** Maps a single reward group (with options/grants) to the read DTO. */
    public RewardGroupDto toRewardGroupDto(ClassLevelRewardGroup group, String lang) {
        return mapGroup(group, lang, buildCache(collectGrants(List.of(group))));
    }

    private List<ClassLevelRewardGrant> collectGrants(List<ClassLevelRewardGroup> groups) {
        List<ClassLevelRewardGrant> grants = new ArrayList<>();
        for (ClassLevelRewardGroup group : groups) {
            grants.addAll(group.getGrants());
            for (ClassLevelRewardOption option : group.getOptions()) {
                grants.addAll(option.getGrants());
            }
        }
        return grants;
    }

    private RewardGroupDto mapGroup(ClassLevelRewardGroup g, String lang, GrantDetailCache cache) {
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
                .options(g.getOptions().stream().map(o -> mapOption(o, lang, cache)).toList())
                .grants(g.getGrants().stream().map(gr -> mapGrant(gr, lang, cache)).toList())
                .build();
    }

    private RewardOptionDto mapOption(ClassLevelRewardOption o, String lang, GrantDetailCache cache) {
        return RewardOptionDto.builder()
                .id(o.getId())
                .optionKey(o.getOptionKey())
                .label(Localization.pick(lang, o.getLabelRu(), o.getLabelEn(), o.getLabelEn()))
                .labelRu(o.getLabelRu())
                .labelEn(o.getLabelEn())
                .description(o.getDescription())
                .recommended(o.getRecommended())
                .sortOrder(o.getSortOrder())
                .grants(o.getGrants().stream().map(gr -> mapGrant(gr, lang, cache)).toList())
                .build();
    }

    private RewardGrantDto mapGrant(ClassLevelRewardGrant grant, String lang, GrantDetailCache cache) {
        return RewardGrantDto.builder()
                .id(grant.getId())
                .grantType(grant.getGrantType())
                .label(Localization.pick(lang, grant.getLabelRu(), grant.getLabelEn(), grant.getLabelEn()))
                .labelRu(grant.getLabelRu())
                .labelEn(grant.getLabelEn())
                .description(grant.getDescription())
                .sortOrder(grant.getSortOrder())
                .payload(mapPayload(grant, lang, cache))
                .build();
    }

    private GrantPayload mapPayload(ClassLevelRewardGrant grant, String lang, GrantDetailCache cache) {
        UUID id = grant.getId();
        GrantType type = GrantType.fromTextOrCustom(grant.getGrantType());
        return switch (type) {
            case FEATURE -> {
                ClassLevelRewardGrantFeature d = cache.features.get(id);
                yield d == null ? customFallback(grant) : FeatureGrantPayload.builder()
                        .featureId(d.getClassFeature() != null ? d.getClassFeature().getId() : null)
                        .build();
            }
            case SUBCLASS -> {
                ClassLevelRewardGrantSubclass d = cache.subclasses.get(id);
                yield d == null ? customFallback(grant) : SubclassGrantPayload.builder()
                        .subclassId(d.getSubclass() != null ? d.getSubclass().getId() : null)
                        .subclass(d.getSubclass() != null ? subclassLabel(d.getSubclass(), lang) : null)
                        .build();
            }
            case FEAT -> {
                ClassLevelRewardGrantFeat d = cache.feats.get(id);
                yield d == null
                        ? FeatGrantPayload.builder().mode("ANY").chooseCount(1).build()
                        : FeatGrantPayload.builder().mode("FIXED")
                                .featId(d.getFeat() != null ? d.getFeat().getId() : null).build();
            }
            case SPELL -> {
                ClassLevelRewardGrantSpell d = cache.spells.get(id);
                if (d == null) {
                    yield customFallback(grant);
                }
                if (d.getSpell() != null) {
                    yield SpellGrantPayload.builder().mode("FIXED")
                            .fixedSpellIds(List.of(d.getSpell().getId())).build();
                }
                yield SpellGrantPayload.builder().mode("CHOICE")
                        .spellLevel(d.getSpellLevel())
                        .schoolIds(d.getSchool() != null ? List.of(d.getSchool().getId()) : null)
                        .chooseCount(d.getChooseCount())
                        .build();
            }
            case SKILL_PROFICIENCY -> {
                ClassLevelRewardGrantSkillProficiency d = cache.skills.get(id);
                if (d == null) {
                    yield customFallback(grant);
                }
                if (Boolean.TRUE.equals(d.getAnySkill())) {
                    yield SkillProficiencyGrantPayload.builder().mode("ANY").chooseCount(d.getChooseCount())
                            .grantsExpertise(d.getGrantsExpertise()).build();
                }
                if (d.getSkillOptions() != null && !d.getSkillOptions().isEmpty()) {
                    yield SkillProficiencyGrantPayload.builder().mode("CHOICE").chooseCount(d.getChooseCount())
                            .skillOptionIds(d.getSkillOptions().stream().map(ContentSkill::getId).toList())
                            .grantsExpertise(d.getGrantsExpertise()).build();
                }
                yield SkillProficiencyGrantPayload.builder().mode("FIXED")
                        .skillIds(d.getSkill() != null ? List.of(d.getSkill().getId()) : List.of())
                        .grantsExpertise(d.getGrantsExpertise()).build();
            }
            case ABILITY_SCORE -> {
                ClassLevelRewardGrantAbilityScore d = cache.abilities.get(id);
                yield d == null ? customFallback(grant) : AbilityScoreGrantPayload.builder()
                        .chooseCount(d.getChooseCount())
                        .bonusPerChoice(d.getBonusPerChoice())
                        .totalBonus(d.getTotalBonus())
                        .maxPerAbility(d.getMaxPerAbility())
                        .maxScore(d.getMaxScore())
                        .abilityOptionIds(abilityOptionIds(d))
                        .build();
            }
            case NUMERIC_MODIFIER -> {
                ClassLevelRewardGrantNumericModifier d = cache.numerics.get(id);
                yield d == null ? customFallback(grant) : NumericModifierGrantPayload.builder()
                        .modifierKey(d.getModifierKey())
                        .amount(d.getAmount() != null ? d.getAmount().intValue() : null)
                        .unitText(d.getUnitText())
                        .durationText(d.getDurationText())
                        .build();
            }
            case CUSTOM_TEXT -> {
                ClassLevelRewardGrantCustomText d = cache.customs.get(id);
                yield d == null ? customFallback(grant) : CustomTextGrantPayload.builder()
                        .title(Localization.pick(lang, d.getTitleRu(), d.getTitleEn(), d.getTitleEn()))
                        .body(d.getBody())
                        .userEditable(d.getUserEditable())
                        .build();
            }
        };
    }

    /**
     * Batch-loads all typed grant detail rows for the given grants in one query per type,
     * avoiding the per-grant N+1 lookups.
     */
    private GrantDetailCache buildCache(Collection<ClassLevelRewardGrant> grants) {
        List<UUID> ids = grants.stream().map(ClassLevelRewardGrant::getId).toList();
        if (ids.isEmpty()) {
            return GrantDetailCache.empty();
        }
        return new GrantDetailCache(
                index(grantFeatureRepo.findAllById(ids), ClassLevelRewardGrantFeature::getId),
                index(grantSubclassRepo.findAllById(ids), ClassLevelRewardGrantSubclass::getId),
                index(grantFeatRepo.findAllById(ids), ClassLevelRewardGrantFeat::getId),
                index(grantSpellRepo.findAllById(ids), ClassLevelRewardGrantSpell::getId),
                index(grantSkillRepo.findAllById(ids), ClassLevelRewardGrantSkillProficiency::getId),
                index(grantAbilityRepo.findAllById(ids), ClassLevelRewardGrantAbilityScore::getId),
                index(grantNumericRepo.findAllById(ids), ClassLevelRewardGrantNumericModifier::getId),
                index(grantCustomRepo.findAllById(ids), ClassLevelRewardGrantCustomText::getId));
    }

    private <T> Map<UUID, T> index(List<T> items, Function<T, UUID> idFn) {
        return items.stream().collect(Collectors.toMap(idFn, Function.identity(), (a, b) -> a));
    }

    /** Preloaded typed grant detail rows keyed by grant id. */
    private record GrantDetailCache(
            Map<UUID, ClassLevelRewardGrantFeature> features,
            Map<UUID, ClassLevelRewardGrantSubclass> subclasses,
            Map<UUID, ClassLevelRewardGrantFeat> feats,
            Map<UUID, ClassLevelRewardGrantSpell> spells,
            Map<UUID, ClassLevelRewardGrantSkillProficiency> skills,
            Map<UUID, ClassLevelRewardGrantAbilityScore> abilities,
            Map<UUID, ClassLevelRewardGrantNumericModifier> numerics,
            Map<UUID, ClassLevelRewardGrantCustomText> customs) {
        static GrantDetailCache empty() {
            return new GrantDetailCache(Map.of(), Map.of(), Map.of(), Map.of(),
                    Map.of(), Map.of(), Map.of(), Map.of());
        }
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
