package com.dnd.app.service;

import com.dnd.app.domain.StatType;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.content.ClassLevelRewardGrant;
import com.dnd.app.domain.content.ClassLevelRewardGroup;
import com.dnd.app.domain.content.ClassLevelRewardOption;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.ContentSubclass;
import com.dnd.app.dto.content.ContentDataAuditReport;
import com.dnd.app.dto.content.ContentDataQualityReport;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ContentDataAuditService: отчёт о полноте данных контент-модели")
class ContentDataAuditServiceTest {

    @Mock private ContentCharacterClassRepository classRepo;
    @Mock private ContentSubclassRepository subclassRepo;
    @Mock private ClassFeatureRepository featureRepo;
    @Mock private ClassLevelRewardGroupRepository groupRepo;
    @Mock private ClassLevelRewardGrantRepository grantRepo;
    @Mock private ClassLevelRewardGrantFeatureRepository grantFeatureRepo;
    @Mock private ClassLevelRewardGrantSubclassRepository grantSubclassRepo;
    @Mock private ClassLevelRewardGrantFeatRepository grantFeatRepo;
    @Mock private ClassLevelRewardGrantSpellRepository grantSpellRepo;
    @Mock private ClassLevelRewardGrantSkillProficiencyRepository grantSkillRepo;
    @Mock private ClassLevelRewardGrantAbilityScoreRepository grantAbilityRepo;
    @Mock private ClassLevelRewardGrantNumericModifierRepository grantNumericRepo;
    @Mock private ClassLevelRewardGrantCustomTextRepository grantCustomRepo;
    @Mock private ImportWarningRepository importWarningRepo;

    @InjectMocks private ContentDataAuditService service;

    @Test
    @DisplayName("Полностью укомплектованный класс не попадает в списки пробелов")
    void completeClass_hasNoGaps() {
        UUID classId = UUID.randomUUID();
        StatType str = StatType.builder().id(UUID.randomUUID()).slug("str").nameRu("Сила").nameEn("Strength").build();
        ContentCharacterClass c = ContentCharacterClass.builder()
                .id(classId).slug("fighter").nameRu("Воин").nameEn("Fighter")
                .hitDie(10).primaryAbilities(Set.of(str)).build();

        ClassLevelRewardGrant grant = ClassLevelRewardGrant.builder().grantType("SUBCLASS").build();
        ClassLevelRewardOption option = ClassLevelRewardOption.builder().grants(List.of(grant)).build();
        ClassLevelRewardGroup group = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).groupKind("CHOICE").options(List.of(option)).build();

        when(classRepo.findAllByHomebrewIsNull()).thenReturn(List.of(c));
        when(featureRepo.findAllByCharacterClassIdOrderByLevelAscSortOrderAsc(classId))
                .thenReturn(List.of(ClassFeature.builder().id(UUID.randomUUID()).build()));
        when(subclassRepo.findAllByCharacterClassId(classId))
                .thenReturn(List.of(ContentSubclass.builder().id(UUID.randomUUID()).build()));
        when(groupRepo.findAllByCharacterClassIdOrderByClassLevelAscSortOrderAsc(classId))
                .thenReturn(List.of(group));

        ContentDataAuditReport report = service.buildReport("en");

        assertEquals(1, report.getCoreClassCount());
        assertTrue(report.getClassesMissingMechanics().isEmpty());
        assertTrue(report.getClassesWithoutFeatures().isEmpty());
        assertTrue(report.getClassesWithoutRewardGroups().isEmpty());
        assertTrue(report.getClassesWithoutSubclassChoice().isEmpty());
        assertTrue(report.getChoiceGroupsWithoutOptions().isEmpty());
        assertTrue(report.getClasses().get(0).isHasSubclassChoiceGroup());
    }

    @Test
    @DisplayName("Пробелы трекаются: нет механик, нет фич, нет групп, нет subclass-choice")
    void incompleteClass_gapsTracked() {
        UUID classId = UUID.randomUUID();
        ContentCharacterClass c = ContentCharacterClass.builder()
                .id(classId).slug("ghost").nameRu("Призрак").nameEn("Ghost")
                .primaryAbilities(Set.of()).build(); // no hitDie, no primaries

        when(classRepo.findAllByHomebrewIsNull()).thenReturn(List.of(c));
        when(featureRepo.findAllByCharacterClassIdOrderByLevelAscSortOrderAsc(classId)).thenReturn(List.of());
        when(subclassRepo.findAllByCharacterClassId(classId)).thenReturn(List.of());
        when(groupRepo.findAllByCharacterClassIdOrderByClassLevelAscSortOrderAsc(classId)).thenReturn(List.of());

        ContentDataAuditReport report = service.buildReport("en");

        assertTrue(report.getClassesMissingMechanics().contains("ghost"));
        assertTrue(report.getClassesWithoutFeatures().contains("ghost"));
        assertTrue(report.getClassesWithoutRewardGroups().contains("ghost"));
        assertTrue(report.getClassesWithoutSubclassChoice().contains("ghost"));
        assertFalse(report.getClasses().get(0).isHasMechanics());
    }

    @Test
    @DisplayName("CHOICE-группа без опций попадает в choiceGroupsWithoutOptions")
    void emptyChoiceGroup_flagged() {
        UUID classId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        StatType str = StatType.builder().id(UUID.randomUUID()).slug("str").nameRu("Сила").nameEn("Strength").build();
        ContentCharacterClass c = ContentCharacterClass.builder()
                .id(classId).slug("fighter").nameRu("Воин").nameEn("Fighter")
                .hitDie(10).primaryAbilities(Set.of(str)).build();
        ClassLevelRewardGroup emptyChoice = ClassLevelRewardGroup.builder()
                .id(groupId).groupKind("CHOICE").options(List.of()).build();

        when(classRepo.findAllByHomebrewIsNull()).thenReturn(List.of(c));
        when(featureRepo.findAllByCharacterClassIdOrderByLevelAscSortOrderAsc(classId)).thenReturn(List.of());
        when(subclassRepo.findAllByCharacterClassId(classId)).thenReturn(List.of());
        when(groupRepo.findAllByCharacterClassIdOrderByClassLevelAscSortOrderAsc(classId))
                .thenReturn(List.of(emptyChoice));

        ContentDataAuditReport report = service.buildReport("en");

        assertTrue(report.getChoiceGroupsWithoutOptions().contains(groupId));
    }

    @Test
    @DisplayName("Data-quality: orphan-грант и грант без typed payload помечаются")
    void dataQuality_flagsOrphanAndMissingTypedPayload() {
        when(classRepo.findAllByHomebrewIsNull()).thenReturn(List.of());

        UUID orphanId = UUID.randomUUID();
        ClassLevelRewardGrant orphan = ClassLevelRewardGrant.builder()
                .id(orphanId).grantType("CUSTOM_TEXT").build(); // no group & no option

        UUID missingId = UUID.randomUUID();
        ClassLevelRewardGroup group = ClassLevelRewardGroup.builder().id(UUID.randomUUID()).build();
        ClassLevelRewardGrant missingTyped = ClassLevelRewardGrant.builder()
                .id(missingId).grantType("ABILITY_SCORE").rewardGroup(group).build();

        when(grantRepo.findAll()).thenReturn(List.of(orphan, missingTyped));
        when(grantAbilityRepo.existsById(missingId)).thenReturn(false);

        ContentDataQualityReport report = service.buildDataQualityReport();

        assertTrue(report.getOrphanGrants().contains(orphanId));
        assertTrue(report.getGrantsWithoutTypedPayload().contains(missingId));
        assertFalse(report.getGrantsWithoutTypedPayload().contains(orphanId),
                "CUSTOM_TEXT всегда считается представленным");
    }
}
