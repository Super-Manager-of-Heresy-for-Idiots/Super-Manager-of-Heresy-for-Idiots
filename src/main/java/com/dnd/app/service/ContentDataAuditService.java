package com.dnd.app.service;

import com.dnd.app.domain.content.ClassLevelRewardGroup;
import com.dnd.app.domain.content.ClassLevelRewardOption;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.dto.content.ContentDataAuditReport;
import com.dnd.app.dto.content.grant.GrantType;
import com.dnd.app.repository.ClassFeatureRepository;
import com.dnd.app.repository.ClassLevelRewardGroupRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.ContentSubclassRepository;
import com.dnd.app.util.Localization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Produces a data-completeness report for the new content model so migration gaps
 * (missing mechanics, unseeded features, classes without reward groups, empty CHOICE
 * groups) are visible without DB access. Read-only.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentDataAuditService {

    private final ContentCharacterClassRepository classRepo;
    private final ContentSubclassRepository subclassRepo;
    private final ClassFeatureRepository featureRepo;
    private final ClassLevelRewardGroupRepository groupRepo;

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
}
