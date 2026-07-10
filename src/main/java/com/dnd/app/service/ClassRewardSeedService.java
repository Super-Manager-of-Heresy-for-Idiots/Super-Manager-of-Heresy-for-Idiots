package com.dnd.app.service;

import com.dnd.app.domain.content.ClassLevelRewardGrant;
import com.dnd.app.domain.content.ClassLevelRewardGrantSubclass;
import com.dnd.app.domain.content.ClassLevelRewardGroup;
import com.dnd.app.domain.content.ClassLevelRewardOption;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.ContentSubclass;
import com.dnd.app.dto.content.ContentSeedSummary;
import com.dnd.app.dto.content.grant.GrantType;
import com.dnd.app.repository.ClassLevelRewardGrantRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSubclassRepository;
import com.dnd.app.repository.ClassLevelRewardGroupRepository;
import com.dnd.app.repository.ClassLevelRewardOptionRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.ContentSubclassRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Класс ClassRewardSeedService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClassRewardSeedService {

    /** PHB 2024: all core classes select their subclass at level 3. */
    static final int SUBCLASS_CHOICE_LEVEL = 3;

    private final ContentCharacterClassRepository classRepo;
    private final ContentSubclassRepository subclassRepo;
    private final ClassLevelRewardGroupRepository groupRepo;
    private final ClassLevelRewardOptionRepository optionRepo;
    private final ClassLevelRewardGrantRepository grantRepo;
    private final ClassLevelRewardGrantSubclassRepository subclassGrantRepo;

    /**
     * Выполняет операции "seed core subclass choice groups" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public ContentSeedSummary seedCoreSubclassChoiceGroups() {
        List<ContentCharacterClass> coreClasses = classRepo.findAllByHomebrewIsNull();
        List<String> created = new ArrayList<>();
        List<String> alreadyPresent = new ArrayList<>();

        for (ContentCharacterClass clazz : coreClasses) {
            if (hasSubclassChoiceGroup(clazz.getId())) {
                alreadyPresent.add(clazz.getSlug());
                continue;
            }
            List<ContentSubclass> subclasses = subclassRepo.findAllByCharacterClassId(clazz.getId()).stream()
                    .filter(s -> s.getHomebrew() == null)
                    .filter(s -> !Boolean.TRUE.equals(s.getEmptyPlaceholder()))
                    .sorted(Comparator.comparing(ContentSubclass::getSlug,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .toList();
            if (subclasses.isEmpty()) {
                alreadyPresent.add(clazz.getSlug());
                continue;
            }
            createSubclassChoiceGroup(clazz, subclasses);
            created.add(clazz.getSlug());
        }

        log.info("Subclass-choice reward groups backfill: {} created, {} already present",
                created.size(), alreadyPresent.size());
        return ContentSeedSummary.builder()
                .created(created.size())
                .skipped(alreadyPresent.size())
                .createdClassSlugs(created)
                .alreadyPresentClassSlugs(alreadyPresent)
                .build();
    }

    private boolean hasSubclassChoiceGroup(UUID classId) {
        List<ClassLevelRewardGroup> groups = groupRepo
                .findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(classId, SUBCLASS_CHOICE_LEVEL);
        for (ClassLevelRewardGroup g : groups) {
            if (!"CHOICE".equalsIgnoreCase(g.getGroupKind())) {
                continue;
            }
            for (ClassLevelRewardOption option : g.getOptions()) {
                boolean hasSubclassGrant = option.getGrants().stream()
                        .anyMatch(gr -> GrantType.SUBCLASS.name().equalsIgnoreCase(gr.getGrantType()));
                if (hasSubclassGrant) {
                    return true;
                }
            }
        }
        return false;
    }

    private void createSubclassChoiceGroup(ContentCharacterClass clazz, List<ContentSubclass> subclasses) {
        ClassLevelRewardGroup group = groupRepo.save(ClassLevelRewardGroup.builder()
                .characterClass(clazz)
                .classLevel(SUBCLASS_CHOICE_LEVEL)
                .groupKind("CHOICE")
                .promptRu("Выберите подкласс")
                .promptEn("Choose a subclass")
                .chooseMin(1)
                .chooseMax(1)
                .repeatable(false)
                .sortOrder(0)
                .build());

        int sortOrder = 0;
        for (ContentSubclass subclass : subclasses) {
            ClassLevelRewardOption option = optionRepo.save(ClassLevelRewardOption.builder()
                    .rewardGroup(group)
                    .optionKey(subclass.getSlug())
                    .labelRu(subclass.getNameRu())
                    .labelEn(subclass.getNameEn())
                    .recommended(false)
                    .sortOrder(sortOrder++)
                    .build());

            ClassLevelRewardGrant grant = grantRepo.save(ClassLevelRewardGrant.builder()
                    .rewardOption(option)
                    .grantType(GrantType.SUBCLASS.name())
                    .sortOrder(0)
                    .build());

            subclassGrantRepo.save(ClassLevelRewardGrantSubclass.builder()
                    .grant(grant)
                    .subclass(subclass)
                    .build());
        }
    }
}
