package com.dnd.app.mapper;

import com.dnd.app.domain.StatType;
import com.dnd.app.domain.content.ClassFeature;
import com.dnd.app.domain.content.ClassLevelRewardGrant;
import com.dnd.app.domain.content.ClassLevelRewardGrantAbilityScore;
import com.dnd.app.domain.content.ClassLevelRewardGrantCustomText;
import com.dnd.app.domain.content.ClassLevelRewardGrantFeature;
import com.dnd.app.domain.content.ClassLevelRewardGrantSubclass;
import com.dnd.app.domain.content.ClassLevelRewardGroup;
import com.dnd.app.domain.content.ClassLevelRewardOption;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.domain.content.ContentSubclass;
import com.dnd.app.dto.content.ContentClassDetailResponse;
import com.dnd.app.dto.content.RewardGroupDto;
import com.dnd.app.dto.content.grant.AbilityScoreGrantPayload;
import com.dnd.app.dto.content.grant.CustomTextGrantPayload;
import com.dnd.app.dto.content.grant.FeatureGrantPayload;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ContentClassMapper: маппинг новой контент-модели класса в ContentClassDetailResponse")
class ContentClassMapperTest {

    @Mock private ClassFeatureRepository featureRepo;
    @Mock private ClassLevelRewardGroupRepository groupRepo;
    @Mock private ClassLevelRewardGrantFeatureRepository grantFeatureRepo;
    @Mock private ClassLevelRewardGrantSubclassRepository grantSubclassRepo;
    @Mock private ClassLevelRewardGrantFeatRepository grantFeatRepo;
    @Mock private ClassLevelRewardGrantSpellRepository grantSpellRepo;
    @Mock private ClassLevelRewardGrantSkillProficiencyRepository grantSkillRepo;
    @Mock private ClassLevelRewardGrantAbilityScoreRepository grantAbilityRepo;
    @Mock private ClassLevelRewardGrantNumericModifierRepository grantNumericRepo;
    @Mock private ClassLevelRewardGrantCustomTextRepository grantCustomRepo;

    @InjectMocks private ContentClassMapper mapper;

    private UUID classId;

    @BeforeEach
    void setUp() {
        classId = UUID.randomUUID();
        // default: no features, no groups (overridden per test)
        lenient().when(featureRepo.findAllByCharacterClassIdOrderByLevelAscSortOrderAsc(classId))
                .thenReturn(List.of());
        lenient().when(groupRepo.findAllByCharacterClassIdOrderByClassLevelAscSortOrderAsc(classId))
                .thenReturn(List.of());
    }

    private StatType ability(String slug, String ru, String en) {
        return StatType.builder().id(UUID.randomUUID()).slug(slug).nameRu(ru).nameEn(en).build();
    }

    private ContentCharacterClass baseClass() {
        return ContentCharacterClass.builder()
                .id(classId)
                .slug("stormbinder")
                .nameRu("Повелитель бурь")
                .nameEn("Stormbinder")
                .subtitle("Storm caster")
                .hitDie(8)
                .spellcaster(false)
                .hasCantrips(false)
                .halfCaster(false)
                .skillChoiceCount(2)
                .skillChoiceAny(false)
                .armorProficiencyText("Light armor")
                .weaponProficiencyText("Simple weapons")
                .primaryAbilities(Set.of(ability("cha", "Харизма", "Charisma")))
                .savingThrows(Set.of(ability("con", "Телосложение", "Constitution")))
                .skillOptions(Set.of(ContentSkill.builder()
                        .id(UUID.randomUUID()).slug("arcana").nameRu("Магия").nameEn("Arcana").build()))
                .build();
    }

    @Test
    @DisplayName("Маппинг механик класса: hitDie, primary/saving, skill choice, proficiency texts")
    void mapsClassMechanics() {
        ContentClassDetailResponse dto = mapper.toDetail(baseClass(), "en");

        assertEquals("stormbinder", dto.getSlug());
        assertEquals("Stormbinder", dto.getName());
        assertEquals(8, dto.getHitDie());
        assertEquals(1, dto.getPrimaryAbilities().size());
        assertEquals("Charisma", dto.getPrimaryAbilities().get(0).getName());
        assertEquals(1, dto.getSavingThrows().size());
        assertEquals(2, dto.getSkillChoiceCount());
        assertFalse(dto.getSkillChoiceAny());
        assertEquals(1, dto.getSkillOptions().size());
        assertEquals("Light armor", dto.getArmorProficiencyText());
        assertNull(dto.getSpellcasting(), "non-spellcaster => null spellcasting");
        assertTrue(dto.getFeatures().isEmpty());
        assertTrue(dto.getRewardGroups().isEmpty());
    }

    @Test
    @DisplayName("Spellcaster: профиль заполняется, прогрессия FULL для не-half-caster")
    void mapsSpellcastingProfile() {
        ContentCharacterClass c = baseClass();
        c.setSpellcaster(true);
        c.setHasCantrips(true);
        c.setHalfCaster(false);
        c.setSpellcastingAbility(ability("cha", "Харизма", "Charisma"));

        ContentClassDetailResponse dto = mapper.toDetail(c, "en");

        assertEquals("FULL", dto.getSpellcasting().getCasterProgression());
        assertEquals("Charisma", dto.getSpellcasting().getSpellcastingAbility().getName());
        assertTrue(dto.getSpellcasting().getHasCantrips());
    }

    @Test
    @DisplayName("AUTO-группа с FEATURE-грантом маппится в FeatureGrantPayload")
    void mapsAutoFeatureGrant() {
        UUID grantId = UUID.randomUUID();
        UUID featureId = UUID.randomUUID();
        ClassFeature feature = ClassFeature.builder().id(featureId).slug("storm-sense").build();

        ClassLevelRewardGrant grant = ClassLevelRewardGrant.builder()
                .id(grantId).grantType("FEATURE").sortOrder(0).build();
        ClassLevelRewardGroup group = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(1).groupKind("AUTO")
                .chooseMin(0).chooseMax(0).repeatable(false).sortOrder(0)
                .options(List.of()).grants(List.of(grant)).build();

        when(groupRepo.findAllByCharacterClassIdOrderByClassLevelAscSortOrderAsc(classId))
                .thenReturn(List.of(group));
        when(grantFeatureRepo.findById(grantId)).thenReturn(Optional.of(
                ClassLevelRewardGrantFeature.builder().id(grantId).classFeature(feature).build()));

        RewardGroupDto g = mapper.toDetail(baseClass(), "en").getRewardGroups().get(0);

        assertEquals("AUTO", g.getGroupKind());
        assertEquals(1, g.getGrants().size());
        FeatureGrantPayload payload = assertInstanceOf(FeatureGrantPayload.class, g.getGrants().get(0).getPayload());
        assertEquals(featureId, payload.getFeatureId());
    }

    @Test
    @DisplayName("CHOICE-группа: каждая опция несёт SUBCLASS-грант")
    void mapsChoiceSubclassGroup() {
        UUID grantId = UUID.randomUUID();
        ContentSubclass subclass = ContentSubclass.builder()
                .id(UUID.randomUUID()).slug("path-of-thunder").nameRu("Путь грома").nameEn("Path of Thunder").build();

        ClassLevelRewardGrant grant = ClassLevelRewardGrant.builder()
                .id(grantId).grantType("SUBCLASS").sortOrder(0).build();
        ClassLevelRewardOption option = ClassLevelRewardOption.builder()
                .id(UUID.randomUUID()).optionKey("path_of_thunder").labelEn("Path of Thunder")
                .recommended(true).sortOrder(0).grants(List.of(grant)).build();
        ClassLevelRewardGroup group = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(3).groupKind("CHOICE")
                .chooseMin(1).chooseMax(1).repeatable(false).sortOrder(0)
                .options(List.of(option)).grants(List.of()).build();

        when(groupRepo.findAllByCharacterClassIdOrderByClassLevelAscSortOrderAsc(classId))
                .thenReturn(List.of(group));
        when(grantSubclassRepo.findById(grantId)).thenReturn(Optional.of(
                ClassLevelRewardGrantSubclass.builder().id(grantId).subclass(subclass).build()));

        RewardGroupDto g = mapper.toDetail(baseClass(), "en").getRewardGroups().get(0);

        assertEquals("CHOICE", g.getGroupKind());
        assertEquals(1, g.getOptions().size());
        assertTrue(g.getOptions().get(0).getRecommended());
        SubclassGrantPayload payload = assertInstanceOf(SubclassGrantPayload.class,
                g.getOptions().get(0).getGrants().get(0).getPayload());
        assertEquals(subclass.getId(), payload.getSubclassId());
    }

    @Test
    @DisplayName("ABILITY_SCORE-грант маппится в AbilityScoreGrantPayload (ASI +2)")
    void mapsAbilityScoreGrant() {
        UUID grantId = UUID.randomUUID();
        ClassLevelRewardGrant grant = ClassLevelRewardGrant.builder()
                .id(grantId).grantType("ABILITY_SCORE").sortOrder(0).build();
        ClassLevelRewardGroup group = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(4).groupKind("AUTO")
                .chooseMin(0).chooseMax(0).repeatable(false).sortOrder(0)
                .options(List.of()).grants(List.of(grant)).build();

        when(groupRepo.findAllByCharacterClassIdOrderByClassLevelAscSortOrderAsc(classId))
                .thenReturn(List.of(group));
        when(grantAbilityRepo.findById(grantId)).thenReturn(Optional.of(
                ClassLevelRewardGrantAbilityScore.builder()
                        .id(grantId).chooseCount(1).bonusPerChoice(2).totalBonus(2).maxPerAbility(2).maxScore(20)
                        .build()));

        RewardGroupDto g = mapper.toDetail(baseClass(), "en").getRewardGroups().get(0);
        AbilityScoreGrantPayload payload = assertInstanceOf(AbilityScoreGrantPayload.class,
                g.getGrants().get(0).getPayload());
        assertEquals(1, payload.getChooseCount());
        assertEquals(2, payload.getBonusPerChoice());
        assertEquals(20, payload.getMaxScore());
    }

    @Test
    @DisplayName("CUSTOM_TEXT-грант маппится в CustomTextGrantPayload с body")
    void mapsCustomTextGrant() {
        UUID grantId = UUID.randomUUID();
        ClassLevelRewardGrant grant = ClassLevelRewardGrant.builder()
                .id(grantId).grantType("CUSTOM_TEXT").labelEn("Stormborn").sortOrder(0).build();
        ClassLevelRewardGroup group = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(1).groupKind("AUTO")
                .chooseMin(0).chooseMax(0).repeatable(false).sortOrder(0)
                .options(List.of()).grants(List.of(grant)).build();

        when(groupRepo.findAllByCharacterClassIdOrderByClassLevelAscSortOrderAsc(classId))
                .thenReturn(List.of(group));
        when(grantCustomRepo.findById(grantId)).thenReturn(Optional.of(
                ClassLevelRewardGrantCustomText.builder()
                        .id(grantId).titleEn("Stormborn").body("You can speak with creatures of the air.")
                        .userEditable(true).build()));

        RewardGroupDto g = mapper.toDetail(baseClass(), "en").getRewardGroups().get(0);
        CustomTextGrantPayload payload = assertInstanceOf(CustomTextGrantPayload.class,
                g.getGrants().get(0).getPayload());
        assertEquals("You can speak with creatures of the air.", payload.getBody());
        assertTrue(payload.getUserEditable());
    }

    @Test
    @DisplayName("Неизвестный grantType трактуется как custom/manual (fallback)")
    void mapsUnknownGrantTypeAsCustomFallback() {
        UUID grantId = UUID.randomUUID();
        ClassLevelRewardGrant grant = ClassLevelRewardGrant.builder()
                .id(grantId).grantType("WEIRD_HOMEBREW_TYPE").labelEn("Mystery").description("does something")
                .sortOrder(0).build();
        ClassLevelRewardGroup group = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(1).groupKind("AUTO")
                .chooseMin(0).chooseMax(0).repeatable(false).sortOrder(0)
                .options(List.of()).grants(List.of(grant)).build();

        when(groupRepo.findAllByCharacterClassIdOrderByClassLevelAscSortOrderAsc(classId))
                .thenReturn(List.of(group));
        // unknown type resolves to CUSTOM_TEXT; no custom_text row exists => fallback
        when(grantCustomRepo.findById(grantId)).thenReturn(Optional.empty());

        RewardGroupDto g = mapper.toDetail(baseClass(), "en").getRewardGroups().get(0);
        CustomTextGrantPayload payload = assertInstanceOf(CustomTextGrantPayload.class,
                g.getGrants().get(0).getPayload());
        assertEquals("Mystery", payload.getTitle());
        assertEquals("does something", payload.getBody());
    }
}
