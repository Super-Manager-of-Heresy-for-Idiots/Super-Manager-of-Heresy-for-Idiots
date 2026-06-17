package com.dnd.app.service;

import com.dnd.app.domain.Feat;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.Spell;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.User;
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
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.content.ClassSaveResult;
import com.dnd.app.dto.content.ValidationIssue;
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
import com.dnd.app.dto.request.ClassWriteRequest;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ClassValidationException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.ContentClassMapper;
import com.dnd.app.repository.ClassFeatureRepository;
import com.dnd.app.repository.ClassLevelRewardGrantAbilityScoreRepository;
import com.dnd.app.repository.ClassLevelRewardGrantCustomTextRepository;
import com.dnd.app.repository.ClassLevelRewardGrantFeatRepository;
import com.dnd.app.repository.ClassLevelRewardGrantFeatureRepository;
import com.dnd.app.repository.ClassLevelRewardGrantNumericModifierRepository;
import com.dnd.app.repository.ClassLevelRewardGrantRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSkillProficiencyRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSpellRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSubclassRepository;
import com.dnd.app.repository.ClassLevelRewardGroupRepository;
import com.dnd.app.repository.ClassLevelRewardOptionRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.ContentSkillRepository;
import com.dnd.app.repository.ContentSubclassRepository;
import com.dnd.app.repository.FeatRepository;
import com.dnd.app.repository.HomebrewPackageRepository;
import com.dnd.app.repository.SpellRepository;
import com.dnd.app.repository.StatTypeRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Aggregate class authoring on the new content model (Phase 8). One request describes
 * the whole class graph; the service validates it (structured 422), then persists
 * mechanics + subclasses + features + reward groups/options/typed grants. Used for both
 * admin/core (homebrew = null) and homebrew packages (homebrew = package, ownership
 * enforced). Update replaces the child graph; delete cascades.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassAuthoringService {

    private static final Set<Integer> VALID_HIT_DICE = Set.of(6, 8, 10, 12);

    private final ContentCharacterClassRepository classRepository;
    private final ContentSubclassRepository subclassRepository;
    private final ClassFeatureRepository featureRepository;
    private final ClassLevelRewardGroupRepository groupRepository;
    private final ClassLevelRewardOptionRepository optionRepository;
    private final ClassLevelRewardGrantRepository grantRepository;
    private final ClassLevelRewardGrantFeatureRepository grantFeatureRepository;
    private final ClassLevelRewardGrantSubclassRepository grantSubclassRepository;
    private final ClassLevelRewardGrantFeatRepository grantFeatRepository;
    private final ClassLevelRewardGrantSpellRepository grantSpellRepository;
    private final ClassLevelRewardGrantSkillProficiencyRepository grantSkillRepository;
    private final ClassLevelRewardGrantAbilityScoreRepository grantAbilityRepository;
    private final ClassLevelRewardGrantNumericModifierRepository grantNumericRepository;
    private final ClassLevelRewardGrantCustomTextRepository grantCustomRepository;
    private final StatTypeRepository statTypeRepository;
    private final ContentSkillRepository skillRepository;
    private final FeatRepository featRepository;
    private final SpellRepository spellRepository;
    private final HomebrewPackageRepository packageRepository;
    private final UserRepository userRepository;
    private final ContentClassMapper classMapper;

    // --- public API: admin/core ---

    @org.springframework.cache.annotation.CacheEvict(
            value = com.dnd.app.config.CacheConfig.CONTENT_VANILLA_CLASSES, allEntries = true)
    @Transactional
    public ClassSaveResult createCoreClass(ClassWriteRequest request, String username, String lang) {
        requireAdmin(username);
        return create(null, request, lang);
    }

    @org.springframework.cache.annotation.CacheEvict(
            value = com.dnd.app.config.CacheConfig.CONTENT_VANILLA_CLASSES, allEntries = true)
    @Transactional
    public ClassSaveResult updateCoreClass(UUID classId, ClassWriteRequest request, String username, String lang) {
        requireAdmin(username);
        ContentCharacterClass existing = loadClass(classId);
        if (existing.getHomebrew() != null) {
            throw new ResourceNotFoundException("Класс не найден");
        }
        return update(existing, null, request, lang);
    }

    @org.springframework.cache.annotation.CacheEvict(
            value = com.dnd.app.config.CacheConfig.CONTENT_VANILLA_CLASSES, allEntries = true)
    @Transactional
    public void deleteCoreClass(UUID classId, String username) {
        requireAdmin(username);
        ContentCharacterClass existing = loadClass(classId);
        if (existing.getHomebrew() != null) {
            throw new ResourceNotFoundException("Класс не найден");
        }
        deleteGraph(existing);
        classRepository.delete(existing);
    }

    // --- public API: homebrew package ---

    @Transactional
    public ClassSaveResult createPackageClass(UUID packageId, ClassWriteRequest request, String username, String lang) {
        HomebrewPackage pkg = loadOwnedPackage(packageId, username);
        return create(pkg, request, lang);
    }

    @Transactional
    public ClassSaveResult updatePackageClass(UUID packageId, UUID classId, ClassWriteRequest request,
                                              String username, String lang) {
        HomebrewPackage pkg = loadOwnedPackage(packageId, username);
        ContentCharacterClass existing = loadClass(classId);
        if (existing.getHomebrew() == null || !existing.getHomebrew().getId().equals(pkg.getId())) {
            throw new AccessDeniedException("Этот класс не принадлежит указанному пакету");
        }
        return update(existing, pkg, request, lang);
    }

    @Transactional
    public void deletePackageClass(UUID packageId, UUID classId, String username) {
        HomebrewPackage pkg = loadOwnedPackage(packageId, username);
        ContentCharacterClass existing = loadClass(classId);
        if (existing.getHomebrew() == null || !existing.getHomebrew().getId().equals(pkg.getId())) {
            throw new AccessDeniedException("Этот класс не принадлежит указанному пакету");
        }
        deleteGraph(existing);
        classRepository.delete(existing);
    }

    // --- create / update core ---

    private ClassSaveResult create(HomebrewPackage homebrew, ClassWriteRequest request, String lang) {
        List<ValidationIssue> issues = new ArrayList<>();
        validateStructure(request, issues);
        validateSlugUnique(request, homebrew, null, issues);
        throwIfErrors(issues);

        ContentCharacterClass clazz = classRepository.save(buildClass(new ContentCharacterClass(), request, homebrew));
        persistGraph(clazz, request, homebrew);
        return saveResult(clazz, homebrew, warnings(issues), lang);
    }

    private ClassSaveResult update(ContentCharacterClass existing, HomebrewPackage homebrew,
                                   ClassWriteRequest request, String lang) {
        List<ValidationIssue> issues = new ArrayList<>();
        validateStructure(request, issues);
        validateSlugUnique(request, homebrew, existing.getId(), issues);
        throwIfErrors(issues);

        deleteGraph(existing);
        buildClass(existing, request, homebrew);
        ContentCharacterClass clazz = classRepository.save(existing);
        persistGraph(clazz, request, homebrew);
        return saveResult(clazz, homebrew, warnings(issues), lang);
    }

    // --- class row ---

    private ContentCharacterClass buildClass(ContentCharacterClass clazz, ClassWriteRequest req, HomebrewPackage hb) {
        clazz.setSlug(effectiveSlug(req.getSlug(), req.getName()));
        clazz.setNameRu(req.getNameRu() != null ? req.getNameRu() : req.getName());
        clazz.setNameEn(req.getNameEn() != null ? req.getNameEn() : req.getName());
        clazz.setSubtitle(req.getSubtitle());
        clazz.setHomebrew(hb);
        clazz.setHitDie(req.getHitDie());
        clazz.setSkillChoiceCount(req.getSkillChoiceCount() != null ? req.getSkillChoiceCount() : 0);
        clazz.setSkillChoiceAny(Boolean.TRUE.equals(req.getSkillChoiceAny()));
        clazz.setArmorProficiencyText(req.getArmorProficiencyText());
        clazz.setWeaponProficiencyText(req.getWeaponProficiencyText());
        clazz.setToolProficiencyText(req.getToolProficiencyText());

        clazz.setPrimaryAbilities(new HashSet<>(resolveAbilities(req.getPrimaryAbilityIds())));
        clazz.setSavingThrows(new HashSet<>(resolveAbilities(req.getSavingThrowIds())));
        clazz.setSkillOptions(new HashSet<>(resolveSkills(req.getSkillOptionIds())));

        ClassWriteRequest.SpellcastingProfile sc = req.getSpellcasting();
        if (sc != null) {
            clazz.setSpellcaster(true);
            clazz.setHasCantrips(Boolean.TRUE.equals(sc.getHasCantrips()));
            clazz.setHalfCaster("HALF".equalsIgnoreCase(sc.getCasterProgression()));
            if (sc.getSpellcastingAbilityId() != null) {
                clazz.setSpellcastingAbility(loadAbility(sc.getSpellcastingAbilityId()));
            }
        } else {
            clazz.setSpellcaster(false);
            clazz.setHasCantrips(false);
            clazz.setHalfCaster(false);
            clazz.setSpellcastingAbility(null);
        }
        return clazz;
    }

    // --- child graph ---

    private void persistGraph(ContentCharacterClass clazz, ClassWriteRequest req, HomebrewPackage hb) {
        Map<String, ContentSubclass> subclassByKey = new HashMap<>();
        if (req.getSubclasses() != null) {
            for (ClassWriteRequest.SubclassInput in : req.getSubclasses()) {
                ContentSubclass sub = subclassRepository.save(ContentSubclass.builder()
                        .characterClass(clazz)
                        .slug(effectiveSlug(in.getSlug(), in.getName()))
                        .nameRu(in.getNameRu() != null ? in.getNameRu() : in.getName())
                        .nameEn(in.getNameEn() != null ? in.getNameEn() : in.getName())
                        .emptyPlaceholder(false)
                        .homebrew(hb)
                        .build());
                if (in.getKey() != null) {
                    subclassByKey.put(in.getKey(), sub);
                }
            }
        }

        Map<String, ClassFeature> featureByKey = new HashMap<>();
        if (req.getFeatures() != null) {
            int autoSort = 0;
            for (ClassWriteRequest.FeatureInput in : req.getFeatures()) {
                ContentSubclass sub = resolveSubclass(in.getSubclassId(), in.getSubclassKey(), subclassByKey,
                        "features", false);
                ClassFeature feature = featureRepository.save(ClassFeature.builder()
                        .characterClass(clazz)
                        .subclass(sub)
                        .slug(effectiveSlug(null, in.getTitle()))
                        .level(in.getLevel())
                        .sortOrder(in.getSortOrder() != null ? in.getSortOrder() : autoSort++)
                        .title(in.getTitle())
                        .description(in.getDescription())
                        .build());
                if (in.getKey() != null) {
                    featureByKey.put(in.getKey(), feature);
                }
            }
        }

        if (req.getRewardGroups() != null) {
            for (ClassWriteRequest.RewardGroupInput gIn : req.getRewardGroups()) {
                ClassFeature groupFeature = gIn.getClassFeatureKey() != null
                        ? featureByKey.get(gIn.getClassFeatureKey())
                        : (gIn.getClassFeatureId() != null
                            ? featureRepository.findById(gIn.getClassFeatureId()).orElse(null) : null);

                ClassLevelRewardGroup group = groupRepository.save(ClassLevelRewardGroup.builder()
                        .characterClass(clazz)
                        .classFeature(groupFeature)
                        .classLevel(gIn.getClassLevel())
                        .groupKind(gIn.getGroupKind())
                        .promptRu(gIn.getPrompt())
                        .promptEn(gIn.getPrompt())
                        .description(gIn.getDescription())
                        .chooseMin(gIn.getChooseMin() != null ? gIn.getChooseMin() : 0)
                        .chooseMax(gIn.getChooseMax() != null ? gIn.getChooseMax() : 1)
                        .repeatable(Boolean.TRUE.equals(gIn.getRepeatable()))
                        .sortOrder(gIn.getSortOrder() != null ? gIn.getSortOrder() : 0)
                        .build());

                if (gIn.getGrants() != null) {
                    int order = 0;
                    for (ClassWriteRequest.GrantInput grIn : gIn.getGrants()) {
                        persistGrant(grIn, group, null, order++, hb, subclassByKey, featureByKey);
                    }
                }

                if (gIn.getOptions() != null) {
                    int optSort = 0;
                    for (ClassWriteRequest.RewardOptionInput oIn : gIn.getOptions()) {
                        ClassLevelRewardOption option = optionRepository.save(ClassLevelRewardOption.builder()
                                .rewardGroup(group)
                                .optionKey(oIn.getOptionKey())
                                .labelRu(oIn.getLabelRu() != null ? oIn.getLabelRu() : oIn.getLabel())
                                .labelEn(oIn.getLabelEn() != null ? oIn.getLabelEn() : oIn.getLabel())
                                .description(oIn.getDescription())
                                .recommended(Boolean.TRUE.equals(oIn.getRecommended()))
                                .sortOrder(oIn.getSortOrder() != null ? oIn.getSortOrder() : optSort++)
                                .build());
                        if (oIn.getGrants() != null) {
                            int gOrder = 0;
                            for (ClassWriteRequest.GrantInput grIn : oIn.getGrants()) {
                                persistGrant(grIn, null, option, gOrder++, hb, subclassByKey, featureByKey);
                            }
                        }
                    }
                }
            }
        }
    }

    private void persistGrant(ClassWriteRequest.GrantInput grIn, ClassLevelRewardGroup group,
                              ClassLevelRewardOption option, int sortOrder, HomebrewPackage hb,
                              Map<String, ContentSubclass> subclassByKey, Map<String, ClassFeature> featureByKey) {
        ClassLevelRewardGrant grant = grantRepository.save(ClassLevelRewardGrant.builder()
                .rewardGroup(group)
                .rewardOption(option)
                .grantType(grIn.getGrantType())
                .labelRu(grIn.getLabelRu() != null ? grIn.getLabelRu() : grIn.getLabel())
                .labelEn(grIn.getLabelEn() != null ? grIn.getLabelEn() : grIn.getLabel())
                .description(grIn.getDescription())
                .sortOrder(grIn.getSortOrder() != null ? grIn.getSortOrder() : sortOrder)
                .build());

        GrantType type = GrantType.fromTextOrCustom(grIn.getGrantType());
        GrantPayload payload = grIn.getPayload();
        switch (type) {
            case FEATURE -> persistFeatureGrant(grant, payload, featureByKey);
            case SUBCLASS -> persistSubclassGrant(grant, payload, subclassByKey);
            case FEAT -> persistFeatGrant(grant, payload, hb);
            case SPELL -> persistSpellGrant(grant, payload);
            case SKILL_PROFICIENCY -> persistSkillGrant(grant, payload);
            case ABILITY_SCORE -> persistAbilityGrant(grant, payload);
            case NUMERIC_MODIFIER -> persistNumericGrant(grant, payload);
            case CUSTOM_TEXT -> persistCustomGrant(grant, grIn, payload);
        }
    }

    private void persistFeatureGrant(ClassLevelRewardGrant grant, GrantPayload payload,
                                     Map<String, ClassFeature> featureByKey) {
        if (!(payload instanceof FeatureGrantPayload p)) {
            return;
        }
        ClassFeature feature = p.getFeatureKey() != null ? featureByKey.get(p.getFeatureKey())
                : (p.getFeatureId() != null ? featureRepository.findById(p.getFeatureId()).orElse(null) : null);
        if (feature == null) {
            throw new ClassValidationException(List.of(ValidationIssue.error(
                    "grants", "UNRESOLVED_KEY", "FEATURE-грант не разрешил featureId/featureKey")));
        }
        grantFeatureRepository.save(ClassLevelRewardGrantFeature.builder().grant(grant).classFeature(feature).build());
    }

    private void persistSubclassGrant(ClassLevelRewardGrant grant, GrantPayload payload,
                                      Map<String, ContentSubclass> subclassByKey) {
        if (!(payload instanceof SubclassGrantPayload p)) {
            return;
        }
        ContentSubclass sub = resolveSubclass(p.getSubclassId(), p.getSubclassKey(), subclassByKey, "grants", true);
        grantSubclassRepository.save(ClassLevelRewardGrantSubclass.builder().grant(grant).subclass(sub).build());
    }

    private void persistFeatGrant(ClassLevelRewardGrant grant, GrantPayload payload, HomebrewPackage hb) {
        if (!(payload instanceof FeatGrantPayload p)) {
            return;
        }
        Feat feat = null;
        if (p.getFeatId() != null) {
            feat = featRepository.findById(p.getFeatId())
                    .orElseThrow(() -> new ClassValidationException(List.of(ValidationIssue.error(
                            "grants", "INVALID_REFERENCE", "Черта не найдена: " + p.getFeatId()))));
        } else if (p.getInlineFeat() != null) {
            feat = featRepository.save(Feat.builder()
                    .slug(effectiveSlug(null, p.getInlineFeat().getName()))
                    .nameRu(p.getInlineFeat().getName())
                    .nameEn(p.getInlineFeat().getName())
                    .description(p.getInlineFeat().getDescription())
                    .homebrew(hb)
                    .build());
        }
        if (feat == null) {
            // mode=ANY without a fixed feat: keep as label-only grant (player chooses on level-up)
            return;
        }
        grantFeatRepository.save(ClassLevelRewardGrantFeat.builder().grant(grant).feat(feat).build());
    }

    private void persistSpellGrant(ClassLevelRewardGrant grant, GrantPayload payload) {
        if (!(payload instanceof SpellGrantPayload p)) {
            return;
        }
        Spell spell = null;
        if (p.getFixedSpellIds() != null && !p.getFixedSpellIds().isEmpty()) {
            UUID spellId = p.getFixedSpellIds().get(0);
            spell = spellRepository.findById(spellId)
                    .orElseThrow(() -> new ClassValidationException(List.of(ValidationIssue.error(
                            "grants", "INVALID_REFERENCE", "Заклинание не найдено: " + spellId))));
        }
        grantSpellRepository.save(ClassLevelRewardGrantSpell.builder()
                .grant(grant)
                .spell(spell)
                .spellLevel(p.getSpellLevel())
                .chooseCount(p.getChooseCount() != null ? p.getChooseCount() : 1)
                .build());
    }

    private void persistSkillGrant(ClassLevelRewardGrant grant, GrantPayload payload) {
        if (!(payload instanceof SkillProficiencyGrantPayload p)) {
            return;
        }
        boolean any = "ANY".equalsIgnoreCase(p.getMode());
        Set<ContentSkill> options = new HashSet<>();
        ContentSkill single = null;
        if (p.getSkillOptionIds() != null) {
            options.addAll(resolveSkills(p.getSkillOptionIds()));
        }
        if (p.getSkillIds() != null && !p.getSkillIds().isEmpty()) {
            single = loadSkill(p.getSkillIds().get(0));
        }
        grantSkillRepository.save(ClassLevelRewardGrantSkillProficiency.builder()
                .grant(grant)
                .skill(single)
                .anySkill(any)
                .chooseCount(p.getChooseCount() != null ? p.getChooseCount() : 1)
                .skillOptions(options)
                .build());
    }

    private void persistAbilityGrant(ClassLevelRewardGrant grant, GrantPayload payload) {
        if (!(payload instanceof AbilityScoreGrantPayload p)) {
            return;
        }
        Set<StatType> options = new HashSet<>();
        if (p.getAbilityOptionIds() != null) {
            options.addAll(resolveAbilities(p.getAbilityOptionIds()));
        }
        grantAbilityRepository.save(ClassLevelRewardGrantAbilityScore.builder()
                .grant(grant)
                .chooseCount(p.getChooseCount() != null ? p.getChooseCount() : 1)
                .bonusPerChoice(p.getBonusPerChoice() != null ? p.getBonusPerChoice() : 1)
                .totalBonus(p.getTotalBonus())
                .maxPerAbility(p.getMaxPerAbility())
                .maxScore(p.getMaxScore())
                .abilityOptions(options)
                .build());
    }

    private void persistNumericGrant(ClassLevelRewardGrant grant, GrantPayload payload) {
        if (!(payload instanceof NumericModifierGrantPayload p)) {
            return;
        }
        grantNumericRepository.save(ClassLevelRewardGrantNumericModifier.builder()
                .grant(grant)
                .modifierKey(p.getModifierKey() != null ? p.getModifierKey() : "custom")
                .targetKind("CUSTOM")
                .amount(p.getAmount() != null ? BigDecimal.valueOf(p.getAmount()) : null)
                .unitText(p.getUnitText())
                .durationText(p.getDurationText())
                .stackingRule(Boolean.TRUE.equals(p.getStacking()) ? "STACK" : null)
                .build());
    }

    private void persistCustomGrant(ClassLevelRewardGrant grant, ClassWriteRequest.GrantInput grIn,
                                    GrantPayload payload) {
        String title = grIn.getLabel();
        String body = grIn.getDescription();
        Boolean userEditable = Boolean.TRUE;
        if (payload instanceof CustomTextGrantPayload p) {
            if (p.getTitle() != null) {
                title = p.getTitle();
            }
            if (p.getBody() != null) {
                body = p.getBody();
            }
            if (p.getUserEditable() != null) {
                userEditable = p.getUserEditable();
            }
        }
        grantCustomRepository.save(ClassLevelRewardGrantCustomText.builder()
                .grant(grant)
                .titleRu(title)
                .titleEn(title)
                .body(body != null ? body : "")
                .userEditable(userEditable)
                .build());
    }

    // --- delete graph (children only) ---

    private void deleteGraph(ContentCharacterClass clazz) {
        List<ClassLevelRewardGroup> groups =
                groupRepository.findAllByCharacterClassIdOrderByClassLevelAscSortOrderAsc(clazz.getId());
        for (ClassLevelRewardGroup group : groups) {
            for (ClassLevelRewardOption option : group.getOptions()) {
                for (ClassLevelRewardGrant grant : option.getGrants()) {
                    deleteTypedGrant(grant);
                }
            }
            for (ClassLevelRewardGrant grant : group.getGrants()) {
                deleteTypedGrant(grant);
            }
        }
        // options & grants cascade via group orphanRemoval
        groupRepository.deleteAll(groups);

        featureRepository.deleteAll(featureRepository.findAllByCharacterClassIdOrderByLevelAscSortOrderAsc(clazz.getId()));
        subclassRepository.deleteAll(subclassRepository.findAllByCharacterClassId(clazz.getId()));
    }

    private void deleteTypedGrant(ClassLevelRewardGrant grant) {
        UUID id = grant.getId();
        grantFeatureRepository.deleteById(id);
        grantSubclassRepository.deleteById(id);
        grantFeatRepository.deleteById(id);
        grantSpellRepository.deleteById(id);
        grantSkillRepository.deleteById(id);
        grantAbilityRepository.deleteById(id);
        grantNumericRepository.deleteById(id);
        grantCustomRepository.deleteById(id);
    }

    // --- validation ---

    private void validateStructure(ClassWriteRequest req, List<ValidationIssue> issues) {
        if (req.getHitDie() == null || !VALID_HIT_DICE.contains(req.getHitDie())) {
            issues.add(ValidationIssue.error("hitDie", "INVALID_HIT_DIE", "hitDie должен быть 6/8/10/12"));
        }
        if (req.getPrimaryAbilityIds() == null || req.getPrimaryAbilityIds().isEmpty()) {
            issues.add(ValidationIssue.error("primaryAbilityIds", "MISSING_PRIMARY_ABILITY",
                    "Нужна хотя бы одна основная характеристика"));
        }
        Set<String> subclassKeys = req.getSubclasses() == null ? Set.of()
                : req.getSubclasses().stream().map(ClassWriteRequest.SubclassInput::getKey)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Set<String> featureKeys = req.getFeatures() == null ? Set.of()
                : req.getFeatures().stream().map(ClassWriteRequest.FeatureInput::getKey)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());

        if (req.getRewardGroups() != null) {
            for (int gi = 0; gi < req.getRewardGroups().size(); gi++) {
                ClassWriteRequest.RewardGroupInput g = req.getRewardGroups().get(gi);
                String path = "rewardGroups[" + gi + "]";
                if (g.getClassLevel() == null || g.getClassLevel() < 1 || g.getClassLevel() > 20) {
                    issues.add(ValidationIssue.error(path + ".classLevel", "INVALID_LEVEL", "classLevel вне 1..20"));
                }
                boolean choice = "CHOICE".equalsIgnoreCase(g.getGroupKind());
                boolean auto = "AUTO".equalsIgnoreCase(g.getGroupKind());
                if (!choice && !auto) {
                    issues.add(ValidationIssue.error(path + ".groupKind", "INVALID_GROUP_KIND",
                            "groupKind должен быть AUTO или CHOICE"));
                }
                int min = g.getChooseMin() != null ? g.getChooseMin() : 0;
                int max = g.getChooseMax() != null ? g.getChooseMax() : (choice ? 1 : 0);
                int optionCount = g.getOptions() != null ? g.getOptions().size() : 0;
                if (min < 0 || max < 0 || min > max) {
                    issues.add(ValidationIssue.error(path, "INVALID_CHOICE_BOUNDS", "Неверные chooseMin/chooseMax"));
                }
                if (auto && optionCount > 0) {
                    issues.add(ValidationIssue.error(path, "AUTO_WITH_OPTIONS", "AUTO-группа не может иметь опций"));
                }
                if (choice && optionCount == 0) {
                    issues.add(ValidationIssue.error(path, "CHOICE_WITHOUT_OPTIONS",
                            "CHOICE-группа требует хотя бы одну опцию"));
                }
                if (choice && max > optionCount) {
                    issues.add(ValidationIssue.error(path, "INVALID_CHOICE_BOUNDS", "chooseMax больше числа опций"));
                }
                Set<String> seenOptionKeys = new HashSet<>();
                if (g.getOptions() != null) {
                    for (int oi = 0; oi < g.getOptions().size(); oi++) {
                        ClassWriteRequest.RewardOptionInput o = g.getOptions().get(oi);
                        String oPath = path + ".options[" + oi + "]";
                        if (o.getOptionKey() != null && !seenOptionKeys.add(o.getOptionKey())) {
                            issues.add(ValidationIssue.error(oPath + ".optionKey", "DUPLICATE_OPTION_KEY",
                                    "Дубликат optionKey: " + o.getOptionKey()));
                        }
                        if (o.getGrants() == null || o.getGrants().isEmpty()) {
                            issues.add(ValidationIssue.warning(oPath, "OPTION_WITHOUT_GRANTS",
                                    "Опция без грантов"));
                        }
                        validateGrantKeys(o.getGrants(), oPath, subclassKeys, featureKeys, issues);
                    }
                }
                validateGrantKeys(g.getGrants(), path, subclassKeys, featureKeys, issues);
                if (g.getClassFeatureKey() != null && !featureKeys.contains(g.getClassFeatureKey())) {
                    issues.add(ValidationIssue.error(path + ".classFeatureKey", "UNRESOLVED_KEY",
                            "Не найден featureKey: " + g.getClassFeatureKey()));
                }
            }
        }
        if (req.getFeatures() != null) {
            for (int fi = 0; fi < req.getFeatures().size(); fi++) {
                ClassWriteRequest.FeatureInput f = req.getFeatures().get(fi);
                if (f.getSubclassKey() != null && !subclassKeys.contains(f.getSubclassKey())) {
                    issues.add(ValidationIssue.error("features[" + fi + "].subclassKey", "UNRESOLVED_KEY",
                            "Не найден subclassKey: " + f.getSubclassKey()));
                }
            }
        }
    }

    private void validateGrantKeys(List<ClassWriteRequest.GrantInput> grants, String path,
                                   Set<String> subclassKeys, Set<String> featureKeys, List<ValidationIssue> issues) {
        if (grants == null) {
            return;
        }
        for (int i = 0; i < grants.size(); i++) {
            ClassWriteRequest.GrantInput grant = grants.get(i);
            GrantPayload payload = grant.getPayload();
            String gPath = path + ".grants[" + i + "]";
            if (payload instanceof FeatureGrantPayload p && p.getFeatureKey() != null
                    && !featureKeys.contains(p.getFeatureKey())) {
                issues.add(ValidationIssue.error(gPath + ".featureKey", "UNRESOLVED_KEY",
                        "Не найден featureKey: " + p.getFeatureKey()));
            }
            if (payload instanceof SubclassGrantPayload p && p.getSubclassKey() != null
                    && !subclassKeys.contains(p.getSubclassKey())) {
                issues.add(ValidationIssue.error(gPath + ".subclassKey", "UNRESOLVED_KEY",
                        "Не найден subclassKey: " + p.getSubclassKey()));
            }
        }
    }

    private void validateSlugUnique(ClassWriteRequest req, HomebrewPackage hb, UUID selfId,
                                    List<ValidationIssue> issues) {
        String slug = effectiveSlug(req.getSlug(), req.getName());
        if (hb == null) {
            classRepository.findBySlugAndHomebrewIsNull(slug)
                    .filter(c -> !c.getId().equals(selfId))
                    .ifPresent(c -> issues.add(ValidationIssue.error("slug", "DUPLICATE_SLUG",
                            "slug уже занят: " + slug)));
        } else {
            boolean dup = classRepository.findAllByHomebrewIdIn(Set.of(hb.getId())).stream()
                    .anyMatch(c -> slug.equals(c.getSlug()) && !c.getId().equals(selfId));
            if (dup) {
                issues.add(ValidationIssue.error("slug", "DUPLICATE_SLUG", "slug уже занят в пакете: " + slug));
            }
        }
    }

    private void throwIfErrors(List<ValidationIssue> issues) {
        if (issues.stream().anyMatch(ValidationIssue::isError)) {
            throw new ClassValidationException(issues.stream().filter(ValidationIssue::isError).toList());
        }
    }

    private List<ValidationIssue> warnings(List<ValidationIssue> issues) {
        return issues.stream().filter(i -> !i.isError()).toList();
    }

    // --- resolution helpers ---

    private List<StatType> resolveAbilities(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<StatType> found = statTypeRepository.findAllById(ids);
        if (found.size() != new HashSet<>(ids).size()) {
            throw new ClassValidationException(List.of(ValidationIssue.error(
                    "abilityIds", "INVALID_REFERENCE", "Некоторые характеристики не найдены")));
        }
        return found;
    }

    private StatType loadAbility(UUID id) {
        return statTypeRepository.findById(id).orElseThrow(() -> new ClassValidationException(
                List.of(ValidationIssue.error("spellcastingAbilityId", "INVALID_REFERENCE",
                        "Характеристика не найдена: " + id))));
    }

    private List<ContentSkill> resolveSkills(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<ContentSkill> found = skillRepository.findAllById(ids);
        if (found.size() != new HashSet<>(ids).size()) {
            throw new ClassValidationException(List.of(ValidationIssue.error(
                    "skillIds", "INVALID_REFERENCE", "Некоторые навыки не найдены")));
        }
        return found;
    }

    private ContentSkill loadSkill(UUID id) {
        return skillRepository.findById(id).orElseThrow(() -> new ClassValidationException(
                List.of(ValidationIssue.error("skillIds", "INVALID_REFERENCE", "Навык не найден: " + id))));
    }

    private ContentSubclass resolveSubclass(UUID id, String key, Map<String, ContentSubclass> byKey,
                                            String path, boolean required) {
        if (key != null) {
            ContentSubclass sub = byKey.get(key);
            if (sub == null) {
                throw new ClassValidationException(List.of(ValidationIssue.error(
                        path, "UNRESOLVED_KEY", "Не найден subclassKey: " + key)));
            }
            return sub;
        }
        if (id != null) {
            return subclassRepository.findById(id).orElseThrow(() -> new ClassValidationException(
                    List.of(ValidationIssue.error(path, "INVALID_REFERENCE", "Сабкласс не найден: " + id))));
        }
        if (required) {
            throw new ClassValidationException(List.of(ValidationIssue.error(
                    path, "UNRESOLVED_KEY", "SUBCLASS-грант не задал subclassId/subclassKey")));
        }
        return null;
    }

    // --- ownership / access ---

    private void requireAdmin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только администратор может управлять core-классами");
        }
    }

    private HomebrewPackage loadOwnedPackage(UUID packageId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        HomebrewPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));
        if (user.getRole() != Role.ADMIN && !pkg.getAuthor().getId().equals(user.getId())) {
            throw new AccessDeniedException("Нельзя редактировать чужой homebrew-пакет");
        }
        return pkg;
    }

    private ContentCharacterClass loadClass(UUID classId) {
        return classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Класс не найден"));
    }

    // --- result ---

    private ClassSaveResult saveResult(ContentCharacterClass clazz, HomebrewPackage hb,
                                       List<ValidationIssue> warnings, String lang) {
        String base = hb == null
                ? "/api/admin/character-classes/" + clazz.getId()
                : "/api/homebrew/packages/" + hb.getId() + "/classes/" + clazz.getId();
        return ClassSaveResult.builder()
                .clazz(classMapper.toDetail(clazz, lang))
                .id(clazz.getId())
                .slug(clazz.getSlug())
                .packageId(hb != null ? hb.getId() : null)
                .etag(UUID.randomUUID().toString())
                .createdAt(Instant.now().toString())
                .updatedAt(Instant.now().toString())
                .warnings(warnings)
                .resourceUrl(base)
                .build();
    }

    private String effectiveSlug(String provided, String name) {
        if (provided != null && !provided.isBlank()) {
            return slugify(provided);
        }
        return slugify(name) + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String slugify(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }
        String slug = value.toLowerCase(Locale.ROOT).trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("[\\s-]+", "-")
                .replaceAll("^-|-$", "");
        return slug.isBlank() ? UUID.randomUUID().toString() : slug;
    }
}
