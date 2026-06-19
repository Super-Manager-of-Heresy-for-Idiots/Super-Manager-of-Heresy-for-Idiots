package com.dnd.app.service;

import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.CharacterStat;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.CharacterRewardSelection;
import com.dnd.app.domain.content.ClassLevelRewardGroup;
import com.dnd.app.domain.content.ClassLevelRewardOption;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.content.LevelUpOptionsResponse;
import com.dnd.app.dto.content.RewardGroupDto;
import com.dnd.app.mapper.ContentClassMapper;
import com.dnd.app.repository.CampaignHomebrewRepository;
import com.dnd.app.repository.CharacterClassLevelRepository;
import com.dnd.app.repository.CharacterRewardSelectionRepository;
import com.dnd.app.repository.ClassLevelRewardGroupRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.StatTypeRepository;
import com.dnd.app.repository.UserRepository;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("LevelUpQueryService: новый read-model повышения уровня")
class LevelUpQueryServiceTest {

    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private UserRepository userRepository;
    @Mock private CharacterClassLevelRepository classLevelRepository;
    @Mock private ContentCharacterClassRepository contentClassRepository;
    @Mock private ClassLevelRewardGroupRepository rewardGroupRepository;
    @Mock private CharacterRewardSelectionRepository rewardSelectionRepository;
    @Mock private CampaignHomebrewRepository campaignHomebrewRepository;
    @Mock private StatTypeRepository statTypeRepository;
    @Mock private ContentClassMapper classMapper;
    @Mock private LevelThresholdService thresholdService;
    @Mock private CampaignService campaignService;

    @InjectMocks private LevelUpQueryService service;

    private User player;
    private StatType con;
    private UUID characterId;

    @BeforeEach
    void setUp() {
        characterId = UUID.randomUUID();
        player = User.builder().id(UUID.randomUUID()).username("player1").role(Role.PLAYER).build();
        con = StatType.builder().id(UUID.randomUUID()).slug("con").nameRu("Телосложение").nameEn("Constitution").build();

        lenient().when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        lenient().when(thresholdService.isReadyToLevelUp(anyLong(), anyInt())).thenReturn(true);
        lenient().when(thresholdService.xpToNextLevel(anyLong(), anyInt())).thenReturn(900L);
        lenient().when(statTypeRepository.findAll()).thenReturn(List.of(con));
        lenient().when(rewardSelectionRepository.findAllByCharacterId(characterId)).thenReturn(List.of());
        lenient().when(rewardGroupRepository
                .findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(any(), anyInt())).thenReturn(List.of());
        lenient().when(classMapper.toRewardGroupDto(any(), anyString()))
                .thenAnswer(i -> RewardGroupDto.builder().id(((ClassLevelRewardGroup) i.getArgument(0)).getId()).build());
    }

    private PlayerCharacter character(int totalLevel) {
        PlayerCharacter c = PlayerCharacter.builder()
                .id(characterId).owner(player).totalLevel(totalLevel).experience(1000L).build();
        c.getStats().add(CharacterStat.builder().character(c).statType(con).value(14).build());
        return c;
    }

    private ContentCharacterClass clazz(String nameEn, int hitDie) {
        return ContentCharacterClass.builder()
                .id(UUID.randomUUID()).slug(nameEn.toLowerCase()).nameEn(nameEn).nameRu(nameEn).hitDie(hitDie).build();
    }

    private CharacterClassLevel level(UUID classId, int classLevel) {
        return CharacterClassLevel.builder().characterId(characterId).classId(classId).classLevel(classLevel).build();
    }

    @Test
    @DisplayName("Одноклассовый: уровень 3 даёт subclass-choice группу")
    void singleClass_subclassChoiceLevel() {
        ContentCharacterClass fighter = clazz("Fighter", 10);
        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character(2)));
        when(classLevelRepository.findAllByCharacterId(characterId)).thenReturn(List.of(level(fighter.getId(), 2)));
        when(contentClassRepository.findById(fighter.getId())).thenReturn(Optional.of(fighter));
        when(contentClassRepository.findAllByHomebrewIsNull()).thenReturn(List.of(fighter));
        ClassLevelRewardGroup subclassGroup = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(3).groupKind("CHOICE").build();
        when(rewardGroupRepository.findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(fighter.getId(), 3))
                .thenReturn(List.of(subclassGroup));

        LevelUpOptionsResponse resp = service.getLevelUpOptions(characterId, "player1", "en");

        assertEquals(2, resp.getCurrentTotalLevel());
        assertEquals(1, resp.getAvailableClasses().size());
        LevelUpOptionsResponse.AvailableClassOption opt = resp.getAvailableClasses().get(0);
        assertEquals(2, opt.getCurrentLevelInClass());
        assertEquals(3, opt.getNewLevelInClass());
        assertEquals(1, opt.getRewardGroups().size());
        assertEquals(10, opt.getHpGain().getHitDie());
        assertEquals(2, opt.getHpGain().getConModifier());
        assertEquals(3, opt.getDerived().getNewTotalLevel());
    }

    @Test
    @DisplayName("Мультикласс: текущий класс + доступный новый класс")
    void multiclass_listsExistingAndNewClass() {
        ContentCharacterClass fighter = clazz("Fighter", 10);
        ContentCharacterClass wizard = clazz("Wizard", 6);
        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character(3)));
        when(classLevelRepository.findAllByCharacterId(characterId)).thenReturn(List.of(level(fighter.getId(), 3)));
        when(contentClassRepository.findById(fighter.getId())).thenReturn(Optional.of(fighter));
        when(contentClassRepository.findAllByHomebrewIsNull()).thenReturn(List.of(fighter, wizard));

        LevelUpOptionsResponse resp = service.getLevelUpOptions(characterId, "player1", "en");

        assertEquals(2, resp.getAvailableClasses().size());
        LevelUpOptionsResponse.AvailableClassOption wizardOpt = resp.getAvailableClasses().stream()
                .filter(o -> o.getClassId().equals(wizard.getId())).findFirst().orElseThrow();
        assertEquals(0, wizardOpt.getCurrentLevelInClass());
        assertEquals(1, wizardOpt.getNewLevelInClass());
    }

    @Test
    @DisplayName("ASI-уровень: reward-группа уровня возвращается в опции")
    void asiLevel_groupReturned() {
        ContentCharacterClass fighter = clazz("Fighter", 10);
        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character(3)));
        when(classLevelRepository.findAllByCharacterId(characterId)).thenReturn(List.of(level(fighter.getId(), 3)));
        when(contentClassRepository.findById(fighter.getId())).thenReturn(Optional.of(fighter));
        when(contentClassRepository.findAllByHomebrewIsNull()).thenReturn(List.of(fighter));
        ClassLevelRewardGroup asiGroup = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(4).groupKind("CHOICE").build();
        when(rewardGroupRepository.findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(fighter.getId(), 4))
                .thenReturn(List.of(asiGroup));

        LevelUpOptionsResponse resp = service.getLevelUpOptions(characterId, "player1", "en");

        assertEquals(1, resp.getAvailableClasses().get(0).getRewardGroups().size());
        assertEquals(4, resp.getAvailableClasses().get(0).getNewLevelInClass());
    }

    @Test
    @DisplayName("Уровень без наград: rewardGroups пустой")
    void noRewardsLevel_emptyGroups() {
        ContentCharacterClass fighter = clazz("Fighter", 10);
        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character(4)));
        when(classLevelRepository.findAllByCharacterId(characterId)).thenReturn(List.of(level(fighter.getId(), 4)));
        when(contentClassRepository.findById(fighter.getId())).thenReturn(Optional.of(fighter));
        when(contentClassRepository.findAllByHomebrewIsNull()).thenReturn(List.of(fighter));

        LevelUpOptionsResponse resp = service.getLevelUpOptions(characterId, "player1", "en");

        assertTrue(resp.getAvailableClasses().get(0).getRewardGroups().isEmpty());
    }

    @Test
    @DisplayName("Уже выбранная опция отражается в alreadySelected")
    void alreadySelectedOption_reflected() {
        ContentCharacterClass fighter = clazz("Fighter", 10);
        when(characterRepository.findById(characterId)).thenReturn(Optional.of(character(2)));
        when(classLevelRepository.findAllByCharacterId(characterId)).thenReturn(List.of(level(fighter.getId(), 2)));
        when(contentClassRepository.findById(fighter.getId())).thenReturn(Optional.of(fighter));
        when(contentClassRepository.findAllByHomebrewIsNull()).thenReturn(List.of(fighter));

        ClassLevelRewardGroup group = ClassLevelRewardGroup.builder()
                .id(UUID.randomUUID()).classLevel(3).groupKind("CHOICE").build();
        ClassLevelRewardOption option = ClassLevelRewardOption.builder().id(UUID.randomUUID()).build();
        when(rewardGroupRepository.findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(fighter.getId(), 3))
                .thenReturn(List.of(group));
        CharacterRewardSelection selection = CharacterRewardSelection.builder()
                .id(UUID.randomUUID()).rewardGroup(group).rewardOption(option).build();
        when(rewardSelectionRepository.findAllByCharacterId(characterId)).thenReturn(List.of(selection));

        LevelUpOptionsResponse resp = service.getLevelUpOptions(characterId, "player1", "en");

        List<LevelUpOptionsResponse.SelectedState> selected =
                resp.getAvailableClasses().get(0).getAlreadySelected();
        assertEquals(1, selected.size());
        assertEquals(group.getId(), selected.get(0).getRewardGroupId());
        assertEquals(option.getId(), selected.get(0).getRewardOptionId());
    }
}
