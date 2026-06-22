package com.dnd.app.service;

import com.dnd.app.domain.StatType;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.content.ClassLevelRewardGrant;
import com.dnd.app.domain.content.ClassLevelRewardGrantAbilityScore;
import com.dnd.app.domain.content.ClassLevelRewardGrantSkillProficiency;
import com.dnd.app.domain.content.ClassLevelRewardGrantSpell;
import com.dnd.app.domain.content.ClassLevelRewardGroup;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.repository.ClassFeatureRepository;
import com.dnd.app.repository.ClassLevelRewardGrantAbilityScoreRepository;
import com.dnd.app.repository.ClassLevelRewardGrantFeatureRepository;
import com.dnd.app.repository.ClassLevelRewardGrantRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSkillProficiencyRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSpellRepository;
import com.dnd.app.repository.ClassLevelRewardGroupRepository;
import com.dnd.app.repository.ClassLevelRewardOptionRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.StatTypeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClassLevelRewardSeedService: backfill per-level reward-групп из normalized JSON")
class ClassLevelRewardSeedServiceTest {

    @Mock private ObjectMapper mapper;
    @Mock private ContentCharacterClassRepository classRepo;
    @Mock private ClassFeatureRepository featureRepo;
    @Mock private StatTypeRepository statTypeRepo;
    @Mock private ClassLevelRewardGroupRepository groupRepo;
    @Mock private ClassLevelRewardOptionRepository optionRepo;
    @Mock private ClassLevelRewardGrantRepository grantRepo;
    @Mock private ClassLevelRewardGrantFeatureRepository featureGrantRepo;
    @Mock private ClassLevelRewardGrantAbilityScoreRepository abilityGrantRepo;
    @Mock private ClassLevelRewardGrantSkillProficiencyRepository skillGrantRepo;
    @Mock private ClassLevelRewardGrantSpellRepository spellGrantRepo;

    @InjectMocks private ClassLevelRewardSeedService service;

    private final ObjectMapper real = new ObjectMapper();
    private final UUID classId = UUID.randomUUID();

    // ---- JSON fixtures -------------------------------------------------------

    private ObjectNode valueNumeric(String columnSlug, int num) {
        ObjectNode v = real.createObjectNode();
        v.put("column_slug", columnSlug);
        v.put("value_numeric", num);
        return v;
    }

    private ObjectNode valueRaw(String columnSlug, String raw) {
        ObjectNode v = real.createObjectNode();
        v.put("column_slug", columnSlug);
        v.put("value_raw", raw);
        return v;
    }

    private ObjectNode progRow(int level, ObjectNode... values) {
        ObjectNode row = real.createObjectNode();
        row.put("level", level);
        ArrayNode vs = row.putArray("values");
        for (ObjectNode v : values) {
            vs.add(v);
        }
        return row;
    }

    private ObjectNode feature(int level, String title, String text) {
        ObjectNode f = real.createObjectNode();
        f.put("level", level);
        f.put("title", title);
        if (text != null) {
            f.put("text", text);
        }
        return f;
    }

    private ArrayNode singleClass(List<ObjectNode> progRows, List<ObjectNode> features) {
        ObjectNode c = real.createObjectNode();
        c.put("slug", "test-mage");
        ArrayNode prog = c.putArray("progression");
        progRows.forEach(prog::add);
        ArrayNode feats = c.putArray("features");
        features.forEach(feats::add);
        ArrayNode root = real.createArrayNode();
        root.add(c);
        return root;
    }

    private ContentCharacterClass clazz() {
        return ContentCharacterClass.builder().id(classId).slug("test-mage")
                .nameRu("Маг").nameEn("Mage").build();
    }

    private StatType ability(String slug) {
        return StatType.builder().id(UUID.randomUUID()).slug(slug).nameRu(slug).nameEn(slug).build();
    }

    private void echoFeatureSave() {
        when(featureRepo.findByCharacterClassIdAndSubclassIsNullAndSlug(any(), any()))
                .thenReturn(Optional.empty());
        when(featureRepo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    private void echoRewardSaves() {
        when(groupRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(grantRepo.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    // ---- tests ---------------------------------------------------------------

    @Test
    @DisplayName("Новый класс: базовая фича -> AUTO-группа, прирост подготовленных -> CHOICE-группа заклинаний")
    void seedsFeatureAutoGroupAndSpellChoice() throws Exception {
        ArrayNode root = singleClass(
                List.of(
                        progRow(1, valueNumeric("podgotovlennye-zaklinaniya", 4)),
                        progRow(2,
                                valueRaw("klassovye-umeniya", "Тестовая фича"),
                                valueNumeric("podgotovlennye-zaklinaniya", 5))),
                List.of());
        when(mapper.readTree(any(InputStream.class))).thenReturn(root);
        when(statTypeRepo.findByHomebrewIsNull()).thenReturn(List.of());
        when(classRepo.findBySlugAndHomebrewIsNull("test-mage")).thenReturn(Optional.of(clazz()));
        when(groupRepo.findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(any(), anyInt()))
                .thenReturn(List.of());
        echoFeatureSave();
        echoRewardSaves();
        when(optionRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(featureGrantRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(spellGrantRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.seedCoreLevelRewards();

        // AUTO feature group + CHOICE spell group
        verify(groupRepo, times(2)).save(any());
        verify(featureGrantRepo, times(1)).save(any());
        // только заклинательная группа имеет опцию; ASI/expertise тут нет
        verify(optionRepo, times(1)).save(any());
        verify(abilityGrantRepo, never()).save(any());
        verify(skillGrantRepo, never()).save(any());

        ArgumentCaptor<ClassLevelRewardGrantSpell> cap =
                ArgumentCaptor.forClass(ClassLevelRewardGrantSpell.class);
        verify(spellGrantRepo).save(cap.capture());
        assertEquals(1, cap.getValue().getChooseCount());   // 5 - 4
        org.junit.jupiter.api.Assertions.assertNull(cap.getValue().getSpellLevel()); // подготовленное, не заговор
    }

    @Test
    @DisplayName("Идемпотентность: уровень с уже существующим managed-грантом полностью пропускается")
    void idempotent_skipsAlreadySeededLevel() throws Exception {
        ArrayNode root = singleClass(
                List.of(progRow(2, valueRaw("klassovye-umeniya", "Тестовая фича"))),
                List.of());
        when(mapper.readTree(any(InputStream.class))).thenReturn(root);
        when(statTypeRepo.findByHomebrewIsNull()).thenReturn(List.of());
        when(classRepo.findBySlugAndHomebrewIsNull("test-mage")).thenReturn(Optional.of(clazz()));

        ClassLevelRewardGrant existing = ClassLevelRewardGrant.builder().grantType("FEATURE").build();
        ClassLevelRewardGroup seeded = ClassLevelRewardGroup.builder()
                .groupKind("AUTO").grants(List.of(existing)).options(new ArrayList<>()).build();
        when(groupRepo.findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(any(), anyInt()))
                .thenReturn(List.of());
        when(groupRepo.findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(any(), eq(2)))
                .thenReturn(List.of(seeded));

        service.seedCoreLevelRewards();

        verify(groupRepo, never()).save(any());
        verify(featureRepo, never()).save(any());
    }

    @Test
    @DisplayName("'Увеличение характеристик' -> CHOICE-группа ABILITY_SCORE (+1/+1, 6 характеристик на выбор)")
    void asiFeature_seedsAbilityScoreGroup() throws Exception {
        ArrayNode root = singleClass(
                List.of(progRow(4, valueRaw("klassovye-umeniya", "Увеличение характеристик"))),
                List.of());
        when(mapper.readTree(any(InputStream.class))).thenReturn(root);
        when(statTypeRepo.findByHomebrewIsNull()).thenReturn(List.of(
                ability("sila"), ability("lovkost"), ability("teloslozhenie"),
                ability("intellekt"), ability("mudrost"), ability("harizma")));
        when(classRepo.findBySlugAndHomebrewIsNull("test-mage")).thenReturn(Optional.of(clazz()));
        when(groupRepo.findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(any(), anyInt()))
                .thenReturn(List.of());
        echoFeatureSave();
        echoRewardSaves();
        when(optionRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(abilityGrantRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.seedCoreLevelRewards();

        ArgumentCaptor<ClassLevelRewardGrantAbilityScore> cap =
                ArgumentCaptor.forClass(ClassLevelRewardGrantAbilityScore.class);
        verify(abilityGrantRepo).save(cap.capture());
        assertEquals(2, cap.getValue().getChooseCount());
        assertEquals(6, cap.getValue().getAbilityOptions().size());
        verify(spellGrantRepo, never()).save(any());
    }

    @Test
    @DisplayName("Фича с описанием 'Экспертность' -> CHOICE-группа SKILL_PROFICIENCY с grantsExpertise=true")
    void expertiseFeature_seedsSkillProficiencyWithExpertise() throws Exception {
        ArrayNode root = singleClass(
                List.of(progRow(3, valueRaw("klassovye-umeniya", "Компетентность"))),
                List.of(feature(3, "Компетентность",
                        "Вы получаете Экспертность в выбранном навыке, которым уже владеете.")));
        when(mapper.readTree(any(InputStream.class))).thenReturn(root);
        when(statTypeRepo.findByHomebrewIsNull()).thenReturn(List.of());
        when(classRepo.findBySlugAndHomebrewIsNull("test-mage")).thenReturn(Optional.of(clazz()));
        when(groupRepo.findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(any(), anyInt()))
                .thenReturn(List.of());
        echoFeatureSave();
        echoRewardSaves();
        when(optionRepo.save(any())).thenAnswer(i -> i.getArgument(0));
        when(skillGrantRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        service.seedCoreLevelRewards();

        ArgumentCaptor<ClassLevelRewardGrantSkillProficiency> cap =
                ArgumentCaptor.forClass(ClassLevelRewardGrantSkillProficiency.class);
        verify(skillGrantRepo).save(cap.capture());
        assertTrue(Boolean.TRUE.equals(cap.getValue().getGrantsExpertise()));
        assertTrue(Boolean.TRUE.equals(cap.getValue().getAnySkill()));
        assertEquals(1, cap.getValue().getChooseCount());
        verify(abilityGrantRepo, never()).save(any());
    }
}
