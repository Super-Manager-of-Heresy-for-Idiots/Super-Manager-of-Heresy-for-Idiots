package com.dnd.app.service;

import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.content.ClassLevelRewardGrant;
import com.dnd.app.domain.content.ClassLevelRewardGroup;
import com.dnd.app.domain.content.ClassLevelRewardOption;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.dto.content.ContentDataAuditReport;
import com.dnd.app.dto.content.ContentDataQualityReport;
import com.dnd.app.dto.content.ImportWarningResponse;
import com.dnd.app.dto.content.grant.GrantType;
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
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.ContentSubclassRepository;
import com.dnd.app.repository.ImportWarningRepository;
import com.dnd.app.util.Localization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Класс ContentDataAuditService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentDataAuditService {

    private final ContentCharacterClassRepository classRepo;
    private final ContentSubclassRepository subclassRepo;
    private final ClassFeatureRepository featureRepo;
    private final ClassLevelRewardGroupRepository groupRepo;
    private final ClassLevelRewardGrantRepository grantRepo;
    private final ClassLevelRewardGrantFeatureRepository grantFeatureRepo;
    private final ClassLevelRewardGrantSubclassRepository grantSubclassRepo;
    private final ClassLevelRewardGrantFeatRepository grantFeatRepo;
    private final ClassLevelRewardGrantSpellRepository grantSpellRepo;
    private final ClassLevelRewardGrantSkillProficiencyRepository grantSkillRepo;
    private final ClassLevelRewardGrantAbilityScoreRepository grantAbilityRepo;
    private final ClassLevelRewardGrantNumericModifierRepository grantNumericRepo;
    private final ClassLevelRewardGrantCustomTextRepository grantCustomRepo;
    private final ImportWarningRepository importWarningRepo;

    /**
     * Формирует результат операции "build report" в рамках бизнес-логики домена.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public ContentDataAuditReport buildReport(String lang) {
        String resolvedLang = Localization.normalize(lang);
        List<ContentCharacterClass> coreClasses = classRepo.findAllByHomebrewIsNull();

        List<ContentDataAuditReport.ClassAuditEntry> entries = new ArrayList<>();
        List<String> missingMechanics = new ArrayList<>();
        List<String> withoutFeatures = new ArrayList<>();
        List<String> withoutRewardGroups = new ArrayList<>();
        List<String> withoutSubclassChoice = new ArrayList<>();
        List<UUID> choiceGroupsWithoutOptions = new ArrayList<>();

        for (ContentCharacterClass clazz : coreClasses) {
            UUID classId = clazz.getId();

            boolean hasMechanics = clazz.getHitDie() != null
                    && clazz.getPrimaryAbilities() != null && !clazz.getPrimaryAbilities().isEmpty();
            int featureCount = featureRepo.findAllByCharacterClassIdOrderByLevelAscSortOrderAsc(classId).size();
            int subclassCount = subclassRepo.findAllByCharacterClassId(classId).size();

            List<ClassLevelRewardGroup> groups =
                    groupRepo.findAllByCharacterClassIdOrderByClassLevelAscSortOrderAsc(classId);
            boolean hasSubclassChoice = false;
            for (ClassLevelRewardGroup g : groups) {
                boolean isChoice = "CHOICE".equalsIgnoreCase(g.getGroupKind());
                if (isChoice && g.getOptions().isEmpty()) {
                    choiceGroupsWithoutOptions.add(g.getId());
                }
                if (isChoice && groupHasSubclassGrant(g)) {
                    hasSubclassChoice = true;
                }
            }

            if (!hasMechanics) {
                missingMechanics.add(clazz.getSlug());
            }
            if (featureCount == 0) {
                withoutFeatures.add(clazz.getSlug());
            }
            if (groups.isEmpty()) {
                withoutRewardGroups.add(clazz.getSlug());
            }
            if (!hasSubclassChoice) {
                withoutSubclassChoice.add(clazz.getSlug());
            }

            entries.add(ContentDataAuditReport.ClassAuditEntry.builder()
                    .classId(classId)
                    .slug(clazz.getSlug())
                    .name(Localization.pick(resolvedLang, clazz.getNameRu(), clazz.getNameEn(),
                            clazz.getNameEn() != null ? clazz.getNameEn() : clazz.getNameRu()))
                    .hasMechanics(hasMechanics)
                    .featureCount(featureCount)
                    .rewardGroupCount(groups.size())
                    .subclassCount(subclassCount)
                    .hasSubclassChoiceGroup(hasSubclassChoice)
                    .build());
        }

        return ContentDataAuditReport.builder()
                .coreClassCount(coreClasses.size())
                .classes(entries)
                .classesMissingMechanics(missingMechanics)
                .classesWithoutFeatures(withoutFeatures)
                .classesWithoutRewardGroups(withoutRewardGroups)
                .classesWithoutSubclassChoice(withoutSubclassChoice)
                .choiceGroupsWithoutOptions(choiceGroupsWithoutOptions)
                .build();
    }

    private boolean groupHasSubclassGrant(ClassLevelRewardGroup group) {
        for (ClassLevelRewardOption option : group.getOptions()) {
            boolean match = option.getGrants().stream()
                    .anyMatch(gr -> GrantType.SUBCLASS.name().equalsIgnoreCase(gr.getGrantType()));
            if (match) {
                return true;
            }
        }
        return false;
    }

    // --- Phase 9: deeper data-quality findings ---

    /**
     * Формирует результат операции "build data quality report" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public ContentDataQualityReport buildDataQualityReport() {
        List<ContentDataQualityReport.FeatureGap> featuresWithoutRewards = new ArrayList<>();
        for (ContentCharacterClass clazz : classRepo.findAllByHomebrewIsNull()) {
            List<ClassLevelRewardGroup> groups =
                    groupRepo.findAllByCharacterClassIdOrderByClassLevelAscSortOrderAsc(clazz.getId());
            for (ClassFeature feature : featureRepo.findAllByCharacterClassIdOrderByLevelAscSortOrderAsc(clazz.getId())) {
                boolean referencedByGroup = groups.stream()
                        .anyMatch(g -> g.getClassFeature() != null && g.getClassFeature().getId().equals(feature.getId()));
                boolean grantedByFeatureGrant = !grantFeatureRepo.findAllByClassFeatureId(feature.getId()).isEmpty();
                if (!referencedByGroup && !grantedByFeatureGrant) {
                    featuresWithoutRewards.add(ContentDataQualityReport.FeatureGap.builder()
                            .featureId(feature.getId())
                            .featureSlug(feature.getSlug())
                            .title(feature.getTitle())
                            .classSlug(clazz.getSlug())
                            .build());
                }
            }
        }

        List<UUID> grantsWithoutTypedPayload = new ArrayList<>();
        List<UUID> orphanGrants = new ArrayList<>();
        for (ClassLevelRewardGrant grant : grantRepo.findAll()) {
            if (grant.getRewardGroup() == null && grant.getRewardOption() == null) {
                orphanGrants.add(grant.getId());
            }
            if (!hasTypedPayload(grant)) {
                grantsWithoutTypedPayload.add(grant.getId());
            }
        }

        return ContentDataQualityReport.builder()
                .featuresWithoutRewards(featuresWithoutRewards)
                .grantsWithoutTypedPayload(grantsWithoutTypedPayload)
                .orphanGrants(orphanGrants)
                .build();
    }

    /** A known-typed grant must have its typed row; CUSTOM_TEXT/unknown render as custom and are fine. */
    private boolean hasTypedPayload(ClassLevelRewardGrant grant) {
        UUID id = grant.getId();
        return switch (GrantType.fromTextOrCustom(grant.getGrantType())) {
            case FEATURE -> grantFeatureRepo.existsById(id);
            case SUBCLASS -> grantSubclassRepo.existsById(id);
            // FEAT may be "choose any" (no fixed feat row) — that is a valid manual representation
            case FEAT -> true;
            case SPELL -> grantSpellRepo.existsById(id);
            case SKILL_PROFICIENCY -> grantSkillRepo.existsById(id);
            case ABILITY_SCORE -> grantAbilityRepo.existsById(id);
            case NUMERIC_MODIFIER -> grantNumericRepo.existsById(id);
            case CUSTOM_TEXT -> true;
        };
    }

    /**
     * Возвращает список для операции "list import warnings" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<ImportWarningResponse> listImportWarnings() {
        return importWarningRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(w -> ImportWarningResponse.builder()
                        .id(w.getId())
                        .sourceSlug(w.getSourceSlug())
                        .entityKind(w.getEntityKind())
                        .entitySlug(w.getEntitySlug())
                        .warningCode(w.getWarningCode())
                        .message(w.getMessage())
                        .createdAt(w.getCreatedAt())
                        .build())
                .toList();
    }
}
