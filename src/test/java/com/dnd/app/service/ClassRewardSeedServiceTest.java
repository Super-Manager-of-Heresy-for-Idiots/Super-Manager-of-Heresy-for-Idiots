package com.dnd.app.service;

import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.content.ClassLevelRewardGrant;
import com.dnd.app.domain.content.ClassLevelRewardGrantSubclass;
import com.dnd.app.domain.content.ClassLevelRewardGroup;
import com.dnd.app.domain.content.ClassLevelRewardOption;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.ContentSubclass;
import com.dnd.app.dto.content.ContentSeedSummary;
import com.dnd.app.repository.ClassLevelRewardGrantRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSubclassRepository;
import com.dnd.app.repository.ClassLevelRewardGroupRepository;
import com.dnd.app.repository.ClassLevelRewardOptionRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.ContentSubclassRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClassRewardSeedService: идемпотентный backfill subclass-choice групп")
class ClassRewardSeedServiceTest {

    @Mock private ContentCharacterClassRepository classRepo;
    @Mock private ContentSubclassRepository subclassRepo;
    @Mock private ClassLevelRewardGroupRepository groupRepo;
    @Mock private ClassLevelRewardOptionRepository optionRepo;
    @Mock private ClassLevelRewardGrantRepository grantRepo;
    @Mock private ClassLevelRewardGrantSubclassRepository subclassGrantRepo;

    @InjectMocks private ClassRewardSeedService service;

    private ContentCharacterClass clazz(String slug) {
        return ContentCharacterClass.builder().id(UUID.randomUUID()).slug(slug)
                .nameRu("Воин").nameEn("Fighter").build();
    }

    private ContentSubclass subclass(String slug, boolean placeholder, HomebrewPackage hb) {
        return ContentSubclass.builder().id(UUID.randomUUID()).slug(slug)
                .nameRu(slug).nameEn(slug).emptyPlaceholder(placeholder).homebrew(hb).build();
    }

    private void echoSaves() {
        when(groupRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(optionRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(grantRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(subclassGrantRepo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("Первый запуск создаёт CHOICE-группу с опциями и SUBCLASS-грантами")
    void firstRun_createsGroupOptionsAndGrants() {
        ContentCharacterClass c = clazz("fighter");
        when(classRepo.findAllByHomebrewIsNull()).thenReturn(List.of(c));
        when(groupRepo.findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(c.getId(), 3))
                .thenReturn(List.of());
        when(subclassRepo.findAllByCharacterClassId(c.getId()))
                .thenReturn(List.of(subclass("champion", false, null), subclass("battle-master", false, null)));
        echoSaves();

        ContentSeedSummary summary = service.seedCoreSubclassChoiceGroups();

        assertEquals(1, summary.getCreated());
        assertEquals(0, summary.getSkipped());
        assertTrue(summary.getCreatedClassSlugs().contains("fighter"));
        verify(groupRepo, times(1)).save(any());
        verify(optionRepo, times(2)).save(any());
        verify(grantRepo, times(2)).save(any());
        verify(subclassGrantRepo, times(2)).save(any(ClassLevelRewardGrantSubclass.class));
    }

    @Test
    @DisplayName("Повторный запуск не плодит дубликаты (idempotent): группа с SUBCLASS-грантом уже есть")
    void rerun_isIdempotent() {
        ContentCharacterClass c = clazz("fighter");
        ClassLevelRewardGrant existingGrant = ClassLevelRewardGrant.builder().grantType("SUBCLASS").build();
        ClassLevelRewardOption existingOption = ClassLevelRewardOption.builder()
                .grants(List.of(existingGrant)).build();
        ClassLevelRewardGroup existingGroup = ClassLevelRewardGroup.builder()
                .groupKind("CHOICE").options(List.of(existingOption)).build();

        when(classRepo.findAllByHomebrewIsNull()).thenReturn(List.of(c));
        when(groupRepo.findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(c.getId(), 3))
                .thenReturn(List.of(existingGroup));

        ContentSeedSummary summary = service.seedCoreSubclassChoiceGroups();

        assertEquals(0, summary.getCreated());
        assertEquals(1, summary.getSkipped());
        assertTrue(summary.getAlreadyPresentClassSlugs().contains("fighter"));
        verify(groupRepo, never()).save(any());
        verify(subclassRepo, never()).findAllByCharacterClassId(any());
    }

    @Test
    @DisplayName("Классы без реальных подклассов (placeholder/homebrew) пропускаются")
    void skipsWhenNoRealSubclasses() {
        ContentCharacterClass c = clazz("fighter");
        HomebrewPackage hb = HomebrewPackage.builder().id(UUID.randomUUID()).build();
        when(classRepo.findAllByHomebrewIsNull()).thenReturn(List.of(c));
        when(groupRepo.findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(c.getId(), 3))
                .thenReturn(List.of());
        when(subclassRepo.findAllByCharacterClassId(c.getId()))
                .thenReturn(List.of(subclass("placeholder", true, null), subclass("hb-sub", false, hb)));

        ContentSeedSummary summary = service.seedCoreSubclassChoiceGroups();

        assertEquals(0, summary.getCreated());
        assertEquals(1, summary.getSkipped());
        verify(groupRepo, never()).save(any());
    }

    @Test
    @DisplayName("Гранты используют канонический grantType=SUBCLASS")
    void usesCanonicalSubclassGrantType() {
        ContentCharacterClass c = clazz("wizard");
        when(classRepo.findAllByHomebrewIsNull()).thenReturn(List.of(c));
        when(groupRepo.findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(c.getId(), 3))
                .thenReturn(List.of());
        when(subclassRepo.findAllByCharacterClassId(c.getId()))
                .thenReturn(List.of(subclass("evoker", false, null)));
        echoSaves();

        service.seedCoreSubclassChoiceGroups();

        verify(grantRepo).save(org.mockito.ArgumentMatchers.argThat(
                g -> "SUBCLASS".equals(g.getGrantType())));
        verify(groupRepo).save(org.mockito.ArgumentMatchers.argThat(
                g -> "CHOICE".equals(g.getGroupKind()) && Integer.valueOf(3).equals(g.getClassLevel())));
    }
}
