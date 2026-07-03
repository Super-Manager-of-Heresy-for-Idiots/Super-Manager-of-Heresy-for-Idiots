package com.dnd.app.service;

import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.CharacterStat;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.ProficiencySkill;
import com.dnd.app.domain.Spell;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.CharacterRewardSelection;
import com.dnd.app.domain.content.ClassLevelRewardGrant;
import com.dnd.app.domain.content.ClassLevelRewardGrantAbilityScore;
import com.dnd.app.domain.content.ClassLevelRewardGrantSkillProficiency;
import com.dnd.app.domain.content.ClassLevelRewardGrantSpell;
import com.dnd.app.domain.content.ClassLevelRewardGroup;
import com.dnd.app.domain.content.ClassLevelRewardOption;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.content.LevelUpRequest;
import com.dnd.app.dto.content.LevelUpResultResponse;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.UnprocessableEntityException;
import com.dnd.app.repository.CampaignHomebrewRepository;
import com.dnd.app.repository.CharacterClassLevelRepository;
import com.dnd.app.repository.CharacterKnownSpellRepository;
import com.dnd.app.repository.CharacterRewardAbilityScoreSelectionRepository;
import com.dnd.app.repository.CharacterRewardSelectionRepository;
import com.dnd.app.repository.CharacterRewardSkillSelectionRepository;
import com.dnd.app.repository.CharacterRewardSpellSelectionRepository;
import com.dnd.app.repository.CharacterSkillProficiencyRepository;
import com.dnd.app.repository.CharacterStatRepository;
import com.dnd.app.repository.ClassLevelRewardGrantAbilityScoreRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSkillProficiencyRepository;
import com.dnd.app.repository.ClassLevelRewardGrantSpellRepository;
import com.dnd.app.repository.ClassLevelRewardGroupRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.SpellRepository;
import com.dnd.app.repository.StatTypeRepository;
import com.dnd.app.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LevelUpCommandService: commit повышения уровня на новой модели")
class LevelUpCommandServiceTest {

    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private UserRepository userRepository;
    @Mock private ContentCharacterClassRepository contentClassRepository;
    @Mock private CharacterClassLevelRepository classLevelRepository;
    @Mock private ClassLevelRewardGroupRepository rewardGroupRepository;
    @Mock private CharacterRewardSelectionRepository rewardSelectionRepository;
    @Mock private CharacterRewardAbilityScoreSelectionRepository abilityScoreSelectionRepository;
    @Mock private CharacterRewardSkillSelectionRepository skillSelectionRepository;
    @Mock private CharacterRewardSpellSelectionRepository spellSelectionRepository;
    @Mock private ClassLevelRewardGrantAbilityScoreRepository abilityGrantRepository;
    @Mock private ClassLevelRewardGrantSkillProficiencyRepository skillGrantRepository;
    @Mock private ClassLevelRewardGrantSpellRepository spellGrantRepository;
    @Mock private CharacterStatRepository characterStatRepository;
    @Mock private StatTypeRepository statTypeRepository;
    @Mock private CharacterSkillProficiencyRepository skillProficiencyRepository;
    @Mock private CharacterKnownSpellRepository knownSpellRepository;
    @Mock private SpellRepository spellRepository;
    @Mock private CampaignHomebrewRepository campaignHomebrewRepository;
    @Mock private CampaignService campaignService;
    @Mock private LevelThresholdService thresholdService;
    @Mock private CharacterFeatureGrantService characterFeatureGrantService;
    @Mock private EntityManager entityManager;

    @InjectMocks private LevelUpCommandService service;

    private UUID characterId;
    private User player;
    private StatType con;
    private StatType str;
    private ContentCharacterClass fighter;

    @BeforeEach
    void setUp() {
        characterId = UUID.randomUUID();
        player = User.builder().id(UUID.randomUUID()).username("player1").role(Role.PLAYER).build();
        con = StatType.builder().id(UUID.randomUUID()).slug("con").nameRu("Телосложение").nameEn("Constitution").build();
        str = StatType.builder().id(UUID.randomUUID()).slug("str").nameRu("Сила").nameEn("Strength").build();
        fighter = ContentCharacterClass.builder()
                .id(UUID.randomUUID()).slug("fighter").nameRu("Воин").nameEn("Fighter").hitDie(10).build();

        org.springframework.test.util.ReflectionTestUtils.setField(service, "entityManager", entityManager);

        lenient().when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        lenient().when(thresholdService.isReadyToLevelUp(anyLong(), anyInt())).thenReturn(true);
        lenient().when(statTypeRepository.findAll()).thenReturn(List.of(con, str));
        lenient().when(contentClassRepository.findById(fighter.getId())).thenReturn(Optional.of(fighter));
        lenient().when(classLevelRepository.findByCharacterIdAndClassId(characterId, fighter.getId()))
                .thenReturn(Optional.of(CharacterClassLevel.builder()
                        .characterId(characterId).classId(fighter.getId()).classLevel(2).build()));
        lenient().when(rewardSelectionRepository.findByCharacterIdAndRewardOptionId(eq(characterId), any()))
                .thenReturn(Optional.empty());
        lenient().when(rewardSelectionRepository.save(any())).thenAnswer(i -> {
            CharacterRewardSelection s = i.getArgument(0);
            s.setId(UUID.randomUUID());
            return s;
        });
        lenient().when(characterStatRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(characterRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(abilityScoreSelectionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(skillSelectionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(spellSelectionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(skillProficiencyRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(knownSpellRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(entityManager.getReference(eq(ProficiencySkill.class), any()))
                .thenReturn(org.mockito.Mockito.mock(ProficiencySkill.class));
        lenient().when(entityManager.getReference(eq(ContentSkill.class), any()))
                .thenReturn(org.mockito.Mockito.mock(ContentSkill.class));
        lenient().when(entityManager.getReference(eq(Spell.class), any()))
                .thenReturn(org.mockito.Mockito.mock(Spell.class));
    }

    private PlayerCharacter character() {
        PlayerCharacter c = PlayerCharacter.builder()
                .id(characterId).owner(player).totalLevel(2).experience(1000L).maxHp(20).currentHp(20).build();
        c.getStats().add(CharacterStat.builder().character(c).statType(con).value(14).build());
        c.getStats().add(CharacterStat.builder().character(c).statType(str).value(15).build());
        when(characterRepository.findByIdForUpdate(characterId)).thenReturn(Optional.of(c));
        return c;
    }

    private void stubGroupAtLevel3(ClassLevelRewardGroup group) {
        when(rewardGroupRepository.findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(fighter.getId(), 3))
                .thenReturn(List.of(group));
    }

    private LevelUpRequest request(UUID groupId, List<UUID> optionIds, LevelUpRequest.ChildSelections child) {
        return LevelUpRequest.builder()
                .classId(fighter.getId())
                .selections(List.of(LevelUpRequest.GroupSelection.builder()
                        .rewardGroupId(groupId).optionIds(optionIds).childSelections(child).build()))
                .build();
    }

    @Test
    @DisplayName("Выбор подкласса: selection сохранён, грант SUBCLASS применён")
    void commitSubclassChoice() {
        character();
        UUID grantId = UUID.randomUUID();
        ClassLevelRewardGrant grant = ClassLevelRewardGrant.builder().id(grantId).grantType("SUBCLASS").build();
        ClassLevelRewardOption option = ClassLevelRewardOption.builder()
                .id(UUID.randomUUID()).grants(List.of(grant)).build();
        ClassLevelRewardGroup group = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(3).groupKind("CHOICE").chooseMin(1).chooseMax(1)
                .repeatable(false).options(List.of(option)).build();
        stubGroupAtLevel3(group);

        LevelUpResultResponse result = service.commitLevelUp(
                characterId, "player1", request(group.getId(), List.of(option.getId()), null), "en");

        assertEquals(3, result.getNewClassLevel());
        assertEquals(3, result.getNewTotalLevel());
        assertEquals(1, result.getAppliedGrants().size());
        assertEquals("SUBCLASS", result.getAppliedGrants().get(0).getGrantType());
        verify(rewardSelectionRepository).save(any());
    }

    @Test
    @DisplayName("Initial reward selection: selection сохраняется без повышения уровня")
    void applyInitialRewardSelections_persistsWithoutLevelChange() {
        PlayerCharacter c = PlayerCharacter.builder()
                .id(characterId).owner(player).totalLevel(1).experience(0L).maxHp(12).currentHp(12).build();
        UUID grantId = UUID.randomUUID();
        ClassLevelRewardGrant grant = ClassLevelRewardGrant.builder().id(grantId).grantType("SUBCLASS").build();
        ClassLevelRewardOption option = ClassLevelRewardOption.builder()
                .id(UUID.randomUUID()).grants(List.of(grant)).build();
        ClassLevelRewardGroup group = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(1).groupKind("CHOICE").chooseMin(1).chooseMax(1)
                .repeatable(false).options(List.of(option)).build();
        when(rewardGroupRepository.findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(fighter.getId(), 1))
                .thenReturn(List.of(group));

        LevelUpResultResponse result = service.applyInitialRewardSelections(
                c,
                fighter,
                List.of(LevelUpRequest.GroupSelection.builder()
                        .rewardGroupId(group.getId())
                        .optionIds(List.of(option.getId()))
                        .build()),
                "en");

        assertEquals(1, result.getNewClassLevel());
        assertEquals(1, result.getNewTotalLevel());
        assertEquals(0, result.getHpIncrease());
        assertEquals(1, result.getAppliedGrants().size());
        verify(rewardSelectionRepository).save(any());
        verify(classLevelRepository, never()).save(any());
        verify(characterRepository, never()).save(any());
    }

    @Test
    @DisplayName("ASI +2: характеристика растёт на 2, child-selection сохранён")
    void commitAsiPlus2() {
        PlayerCharacter c = character();
        UUID grantId = UUID.randomUUID();
        ClassLevelRewardGrant grant = ClassLevelRewardGrant.builder().id(grantId).grantType("ABILITY_SCORE").build();
        ClassLevelRewardOption option = ClassLevelRewardOption.builder()
                .id(UUID.randomUUID()).grants(List.of(grant)).build();
        ClassLevelRewardGroup group = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(3).groupKind("CHOICE").chooseMin(1).chooseMax(1)
                .repeatable(false).options(List.of(option)).build();
        stubGroupAtLevel3(group);
        when(abilityGrantRepository.findById(grantId)).thenReturn(Optional.of(
                ClassLevelRewardGrantAbilityScore.builder()
                        .id(grantId).chooseCount(1).bonusPerChoice(2).maxScore(20).abilityOptions(Set.of()).build()));

        LevelUpRequest.ChildSelections child = LevelUpRequest.ChildSelections.builder()
                .abilityScores(List.of(LevelUpRequest.AbilityScoreChoice.builder()
                        .abilityScoreId(str.getId()).amount(2).build())).build();

        service.commitLevelUp(characterId, "player1", request(group.getId(), List.of(option.getId()), child), "en");

        int strValue = c.getStats().stream().filter(s -> s.getStatType().equals(str)).findFirst().orElseThrow().getValue();
        assertEquals(17, strValue);
        verify(abilityScoreSelectionRepository).save(any());
    }

    @Test
    @DisplayName("ASI +1/+1: две характеристики растут на 1")
    void commitAsiPlusOnePlusOne() {
        PlayerCharacter c = character();
        UUID grantId = UUID.randomUUID();
        ClassLevelRewardGrant grant = ClassLevelRewardGrant.builder().id(grantId).grantType("ABILITY_SCORE").build();
        ClassLevelRewardOption option = ClassLevelRewardOption.builder()
                .id(UUID.randomUUID()).grants(List.of(grant)).build();
        ClassLevelRewardGroup group = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(3).groupKind("CHOICE").chooseMin(1).chooseMax(1)
                .repeatable(false).options(List.of(option)).build();
        stubGroupAtLevel3(group);
        when(abilityGrantRepository.findById(grantId)).thenReturn(Optional.of(
                ClassLevelRewardGrantAbilityScore.builder()
                        .id(grantId).chooseCount(2).bonusPerChoice(1).maxScore(20).abilityOptions(Set.of()).build()));

        LevelUpRequest.ChildSelections child = LevelUpRequest.ChildSelections.builder()
                .abilityScores(List.of(
                        LevelUpRequest.AbilityScoreChoice.builder().abilityScoreId(str.getId()).amount(1).build(),
                        LevelUpRequest.AbilityScoreChoice.builder().abilityScoreId(con.getId()).amount(1).build()))
                .build();

        service.commitLevelUp(characterId, "player1", request(group.getId(), List.of(option.getId()), child), "en");

        int strValue = c.getStats().stream().filter(s -> s.getStatType().equals(str)).findFirst().orElseThrow().getValue();
        int conValue = c.getStats().stream().filter(s -> s.getStatType().equals(con)).findFirst().orElseThrow().getValue();
        assertEquals(16, strValue);
        assertEquals(15, conValue);
    }

    @Test
    @DisplayName("Выбор навыков: SKILL_PROFICIENCY применён и сохранён")
    void commitSkillChoices() {
        character();
        UUID grantId = UUID.randomUUID();
        UUID skillId = UUID.randomUUID();
        ContentSkill allowed = ContentSkill.builder().id(skillId).slug("athletics").nameEn("Athletics").build();
        ClassLevelRewardGrant grant = ClassLevelRewardGrant.builder().id(grantId).grantType("SKILL_PROFICIENCY").build();
        ClassLevelRewardOption option = ClassLevelRewardOption.builder()
                .id(UUID.randomUUID()).grants(List.of(grant)).build();
        ClassLevelRewardGroup group = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(3).groupKind("CHOICE").chooseMin(1).chooseMax(1)
                .repeatable(false).options(List.of(option)).build();
        stubGroupAtLevel3(group);
        when(skillGrantRepository.findById(grantId)).thenReturn(Optional.of(
                ClassLevelRewardGrantSkillProficiency.builder()
                        .id(grantId).chooseCount(1).anySkill(false).skillOptions(Set.of(allowed)).build()));

        LevelUpRequest.ChildSelections child = LevelUpRequest.ChildSelections.builder()
                .skillIds(List.of(skillId)).build();

        service.commitLevelUp(characterId, "player1", request(group.getId(), List.of(option.getId()), child), "en");

        verify(skillProficiencyRepository).save(any());
        verify(skillSelectionRepository).save(any());
    }

    @Test
    @DisplayName("Выбор заклинаний: SPELL применён и сохранён")
    void commitSpellChoices() {
        character();
        UUID grantId = UUID.randomUUID();
        UUID spellId = UUID.randomUUID();
        ClassLevelRewardGrant grant = ClassLevelRewardGrant.builder().id(grantId).grantType("SPELL").build();
        ClassLevelRewardOption option = ClassLevelRewardOption.builder()
                .id(UUID.randomUUID()).grants(List.of(grant)).build();
        ClassLevelRewardGroup group = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(3).groupKind("CHOICE").chooseMin(1).chooseMax(1)
                .repeatable(false).options(List.of(option)).build();
        stubGroupAtLevel3(group);
        when(spellGrantRepository.findById(grantId)).thenReturn(Optional.of(
                ClassLevelRewardGrantSpell.builder().id(grantId).chooseCount(1).build()));
        when(spellRepository.findById(spellId)).thenReturn(Optional.of(
                Spell.builder().id(spellId).level(1).build()));

        LevelUpRequest.ChildSelections child = LevelUpRequest.ChildSelections.builder()
                .spellIds(List.of(spellId)).build();

        service.commitLevelUp(characterId, "player1", request(group.getId(), List.of(option.getId()), child), "en");

        verify(knownSpellRepository).save(any());
        verify(spellSelectionRepository).save(any());
    }

    @Test
    @DisplayName("Переизбыток выбора (больше chooseMax) отклоняется")
    void overSelection_rejected() {
        character();
        ClassLevelRewardOption o1 = ClassLevelRewardOption.builder().id(UUID.randomUUID()).grants(List.of()).build();
        ClassLevelRewardOption o2 = ClassLevelRewardOption.builder().id(UUID.randomUUID()).grants(List.of()).build();
        ClassLevelRewardGroup group = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(3).groupKind("CHOICE").chooseMin(1).chooseMax(1)
                .repeatable(false).options(List.of(o1, o2)).build();
        stubGroupAtLevel3(group);

        LevelUpRequest req = request(group.getId(), List.of(o1.getId(), o2.getId()), null);

        assertThrows(UnprocessableEntityException.class,
                () -> service.commitLevelUp(characterId, "player1", req, "en"));
        verify(characterRepository, never()).save(any());
    }

    @Test
    @DisplayName("Повторный выбор той же опции (не repeatable) отклоняется")
    void duplicateSelection_rejected() {
        character();
        ClassLevelRewardOption option = ClassLevelRewardOption.builder().id(UUID.randomUUID()).grants(List.of()).build();
        ClassLevelRewardGroup group = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(3).groupKind("CHOICE").chooseMin(1).chooseMax(1)
                .repeatable(false).options(List.of(option)).build();
        stubGroupAtLevel3(group);
        when(rewardSelectionRepository.findByCharacterIdAndRewardOptionId(characterId, option.getId()))
                .thenReturn(Optional.of(CharacterRewardSelection.builder().id(UUID.randomUUID()).build()));

        LevelUpRequest req = request(group.getId(), List.of(option.getId()), null);

        assertThrows(DuplicateResourceException.class,
                () -> service.commitLevelUp(characterId, "player1", req, "en"));
    }

    @Test
    @DisplayName("Некорректный ASI (неверная прибавка) прерывает commit исключением")
    void invalidAsi_throwsAndDoesNotPersistCharacter() {
        character();
        UUID grantId = UUID.randomUUID();
        ClassLevelRewardGrant grant = ClassLevelRewardGrant.builder().id(grantId).grantType("ABILITY_SCORE").build();
        ClassLevelRewardOption option = ClassLevelRewardOption.builder()
                .id(UUID.randomUUID()).grants(List.of(grant)).build();
        ClassLevelRewardGroup group = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(3).groupKind("CHOICE").chooseMin(1).chooseMax(1)
                .repeatable(false).options(List.of(option)).build();
        stubGroupAtLevel3(group);
        when(abilityGrantRepository.findById(grantId)).thenReturn(Optional.of(
                ClassLevelRewardGrantAbilityScore.builder()
                        .id(grantId).chooseCount(1).bonusPerChoice(2).maxScore(20).abilityOptions(Set.of()).build()));

        // amount 3 != bonusPerChoice 2 -> rejected
        LevelUpRequest.ChildSelections child = LevelUpRequest.ChildSelections.builder()
                .abilityScores(List.of(LevelUpRequest.AbilityScoreChoice.builder()
                        .abilityScoreId(str.getId()).amount(3).build())).build();

        LevelUpRequest req = request(group.getId(), List.of(option.getId()), child);

        assertThrows(UnprocessableEntityException.class,
                () -> service.commitLevelUp(characterId, "player1", req, "en"));
        verify(characterRepository, never()).save(any());
    }
}
