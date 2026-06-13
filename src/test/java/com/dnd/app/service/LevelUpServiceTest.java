package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.LevelUpRequest;
import com.dnd.app.dto.response.LevelUpOptionsResponse;
import com.dnd.app.dto.response.LevelUpResultResponse;
import com.dnd.app.dto.response.RewardDetailDto;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.UnprocessableEntityException;
import com.dnd.app.repository.*;
import com.dnd.app.service.reward.RewardResolverRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LevelUpService: повышение уровня персонажа и выдача наград")
class LevelUpServiceTest {

    @Mock private PlayerCharacterRepository characterRepository;
    @Mock private UserRepository userRepository;
    @Mock private CharacterClassRepository classRepository;
    @Mock private CharacterClassLevelRepository classLevelRepository;
    @Mock private ClassLevelRewardRepository rewardCatalogRepository;
    @Mock private CharacterAcquiredRewardRepository acquiredRewardRepository;
    @Mock private CharacterStatRepository characterStatRepository;
    @Mock private StatTypeRepository statTypeRepository;
    @Mock private CampaignMemberRepository campaignMemberRepository;
    @Mock private CampaignContentService campaignContentService;
    @Mock private RewardResolverRegistry rewardResolverRegistry;
    @Mock private LevelThresholdService thresholdService;
    @Mock private CampaignService campaignService;

    @InjectMocks private LevelUpService levelUpService;

    private User makePlayer(UUID id, String name) {
        return User.builder().id(id).username(name).role(Role.PLAYER).build();
    }

    private PlayerCharacter makeCharacter(UUID id, User owner, int totalLevel, long xp) {
        return PlayerCharacter.builder()
                .id(id).name("Hero").totalLevel(totalLevel).experience(xp)
                .owner(owner).build();
    }

    @Test
    @DisplayName("getLevelUpOptions бросает 409, если персонаж ещё не накопил XP для нового уровня")
    void getLevelUpOptions_whenNotReady_throws409() {
        UUID charId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        User player = makePlayer(playerId, "player1");
        PlayerCharacter character = makeCharacter(charId, player, 1, 0);

        when(characterRepository.findById(charId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        when(thresholdService.isReadyToLevelUp(0L, 1)).thenReturn(false);

        assertThrows(DuplicateResourceException.class,
                () -> levelUpService.getLevelUpOptions(charId, "player1"));
    }

    @Test
    @DisplayName("getLevelUpOptions группирует доступные награды класса по типу (SKILL, FEAT)")
    void getLevelUpOptions_groupsRewardsByType() {
        UUID charId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        User player = makePlayer(playerId, "player1");
        PlayerCharacter character = makeCharacter(charId, player, 1, 300);
        CharacterClass cc = CharacterClass.builder().id(classId).name("Fighter").build();

        CharacterClassLevel ccl = CharacterClassLevel.builder()
                .characterId(charId).classId(classId).classLevel(1).characterClass(cc).build();

        UUID skillRewardId = UUID.randomUUID();
        UUID featRewardId = UUID.randomUUID();
        ClassLevelReward skillReward = ClassLevelReward.builder()
                .id(UUID.randomUUID()).characterClass(cc).requiredLevel(2)
                .rewardType("SKILL").rewardId(skillRewardId).isChoice(false).build();
        ClassLevelReward featReward = ClassLevelReward.builder()
                .id(UUID.randomUUID()).characterClass(cc).requiredLevel(2)
                .rewardType("FEAT").rewardId(featRewardId).isChoice(true).build();

        when(characterRepository.findById(charId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        when(thresholdService.isReadyToLevelUp(300L, 1)).thenReturn(true);
        when(classLevelRepository.findAllByCharacterId(charId)).thenReturn(List.of(ccl));
        when(acquiredRewardRepository.findAllByCharacterId(charId)).thenReturn(List.of());
        when(classRepository.findAllByHomebrewIsNull()).thenReturn(List.of(cc));
        when(rewardCatalogRepository.findAllByCharacterClassIdAndRequiredLevel(classId, 2))
                .thenReturn(List.of(skillReward, featReward));
        when(rewardResolverRegistry.resolve(eq("SKILL"), any())).thenReturn(
                RewardDetailDto.builder().rewardId(skillRewardId).name("Action Surge").description("Extra action").build());
        when(rewardResolverRegistry.resolve(eq("FEAT"), any())).thenReturn(
                RewardDetailDto.builder().rewardId(featRewardId).name("Lucky").description("Reroll").build());

        LevelUpOptionsResponse result = levelUpService.getLevelUpOptions(charId, "player1");

        assertEquals(1, result.getCurrentTotalLevel());
        assertEquals(1, result.getAvailableClasses().size());
        LevelUpOptionsResponse.AvailableClassOption option = result.getAvailableClasses().get(0);
        assertEquals("Fighter", option.getClassName());
        assertEquals(2, option.getRewardGroups().size());
    }

    @Test
    @DisplayName("getLevelUpOptions скрывает группу SUBCLASS, если подкласс уже взят у этого класса")
    void getLevelUpOptions_excludesSubclassGroup_whenAlreadyHasSubclass() {
        UUID charId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        User player = makePlayer(playerId, "player1");
        PlayerCharacter character = makeCharacter(charId, player, 2, 900);
        CharacterClass cc = CharacterClass.builder().id(classId).name("Fighter").build();
        CharacterClassLevel ccl = CharacterClassLevel.builder()
                .characterId(charId).classId(classId).classLevel(2).characterClass(cc).build();

        UUID subRewardId = UUID.randomUUID();
        UUID rewardEntryId = UUID.randomUUID();
        ClassLevelReward subReward = ClassLevelReward.builder()
                .id(rewardEntryId).characterClass(cc).requiredLevel(3)
                .rewardType("SUBCLASS").rewardId(subRewardId).isChoice(true).build();
        CharacterAcquiredReward acquiredSub = CharacterAcquiredReward.builder()
                .id(UUID.randomUUID()).character(character).classLevelReward(subReward).build();

        when(characterRepository.findById(charId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        when(thresholdService.isReadyToLevelUp(900L, 2)).thenReturn(true);
        when(classLevelRepository.findAllByCharacterId(charId)).thenReturn(List.of(ccl));
        when(acquiredRewardRepository.findAllByCharacterId(charId)).thenReturn(List.of(acquiredSub));
        when(classRepository.findAllByHomebrewIsNull()).thenReturn(List.of(cc));
        when(rewardCatalogRepository.findAllByCharacterClassIdAndRequiredLevel(classId, 3))
                .thenReturn(List.of(subReward));

        LevelUpOptionsResponse result = levelUpService.getLevelUpOptions(charId, "player1");

        LevelUpOptionsResponse.AvailableClassOption option = result.getAvailableClasses().get(0);
        assertTrue(option.getRewardGroups().stream()
                .noneMatch(g -> "SUBCLASS".equals(g.getRewardType())));
    }

    @Test
    @DisplayName("commitLevelUp выдаёт выбранную награду-выбор и все авто-награды уровня")
    void commitLevelUp_grantsAllSelectedAndAutoRewards() {
        UUID charId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        User player = makePlayer(playerId, "player1");
        PlayerCharacter character = makeCharacter(charId, player, 1, 300);
        CharacterClass cc = CharacterClass.builder().id(classId).name("Fighter").build();
        CharacterClassLevel ccl = CharacterClassLevel.builder()
                .characterId(charId).classId(classId).classLevel(1).characterClass(cc).build();

        UUID autoSkillId = UUID.randomUUID();
        UUID choiceFeatId = UUID.randomUUID();
        UUID autoEntryId = UUID.randomUUID();
        UUID choiceEntryId = UUID.randomUUID();

        ClassLevelReward autoReward = ClassLevelReward.builder()
                .id(autoEntryId).characterClass(cc).requiredLevel(2)
                .rewardType("SKILL").rewardId(autoSkillId).isChoice(false).build();
        ClassLevelReward choiceReward = ClassLevelReward.builder()
                .id(choiceEntryId).characterClass(cc).requiredLevel(2)
                .rewardType("FEAT").rewardId(choiceFeatId).isChoice(true).build();

        when(characterRepository.findByIdForUpdate(charId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        when(thresholdService.isReadyToLevelUp(300L, 1)).thenReturn(true);
        when(classRepository.findById(classId)).thenReturn(Optional.of(cc));
        when(classLevelRepository.findByCharacterIdAndClassId(charId, classId)).thenReturn(Optional.of(ccl));
        when(rewardCatalogRepository.findAllByCharacterClassIdAndRequiredLevel(classId, 2))
                .thenReturn(List.of(autoReward, choiceReward));
        when(acquiredRewardRepository.findAllByCharacterId(charId)).thenReturn(List.of());
        when(characterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classLevelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(acquiredRewardRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(rewardResolverRegistry.resolve("SKILL", autoSkillId)).thenReturn(
                RewardDetailDto.builder().name("Action Surge").build());
        when(rewardResolverRegistry.resolve("FEAT", choiceFeatId)).thenReturn(
                RewardDetailDto.builder().name("Lucky").build());

        LevelUpRequest req = LevelUpRequest.builder()
                .classId(classId)
                .selections(List.of(
                        LevelUpRequest.RewardSelection.builder()
                                .rewardType("FEAT").rewardEntryId(choiceEntryId).build()
                ))
                .build();

        LevelUpResultResponse result = levelUpService.commitLevelUp(charId, "player1", req);

        assertEquals(2, result.getNewTotalLevel());
        assertEquals("Fighter", result.getClassLeveled());
        assertEquals(2, result.getNewClassLevel());
        assertEquals(2, result.getRewardsAcquired().size());
        verify(acquiredRewardRepository, times(2)).save(any(CharacterAcquiredReward.class));
    }

    @Test
    @DisplayName("commitLevelUp в новый класс создаёт запись уровня класса с classLevel=1")
    void commitLevelUp_newClass_createsClassLevelEntry() {
        UUID charId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID newClassId = UUID.randomUUID();
        User player = makePlayer(playerId, "player1");
        PlayerCharacter character = makeCharacter(charId, player, 1, 300);
        CharacterClass newClass = CharacterClass.builder().id(newClassId).name("Wizard").build();

        when(characterRepository.findByIdForUpdate(charId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        when(thresholdService.isReadyToLevelUp(300L, 1)).thenReturn(true);
        when(classRepository.findById(newClassId)).thenReturn(Optional.of(newClass));
        when(classLevelRepository.findByCharacterIdAndClassId(charId, newClassId)).thenReturn(Optional.empty());
        when(rewardCatalogRepository.findAllByCharacterClassIdAndRequiredLevel(newClassId, 1))
                .thenReturn(List.of());
        when(acquiredRewardRepository.findAllByCharacterId(charId)).thenReturn(List.of());
        when(characterRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(classLevelRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LevelUpRequest req = LevelUpRequest.builder()
                .classId(newClassId).selections(List.of()).build();

        LevelUpResultResponse result = levelUpService.commitLevelUp(charId, "player1", req);

        assertEquals(2, result.getNewTotalLevel());
        assertEquals("Wizard", result.getClassLeveled());
        assertEquals(1, result.getNewClassLevel());
        verify(classLevelRepository).save(argThat(ccl -> ccl.getClassLevel() == 1));
    }

    @Test
    @DisplayName("commitLevelUp молча пропускает выбор второго подкласса, если подкласс уже есть")
    void commitLevelUp_duplicateSubclass_skippedSilently() {
        UUID charId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        User player = makePlayer(playerId, "player1");
        PlayerCharacter character = makeCharacter(charId, player, 2, 900);
        CharacterClass cc = CharacterClass.builder().id(classId).name("Fighter").build();
        CharacterClassLevel ccl = CharacterClassLevel.builder()
                .characterId(charId).classId(classId).classLevel(2).characterClass(cc).build();

        UUID subRewardId1 = UUID.randomUUID();
        UUID subRewardId2 = UUID.randomUUID();
        UUID entryId1 = UUID.randomUUID();
        UUID entryId2 = UUID.randomUUID();

        ClassLevelReward existingSub = ClassLevelReward.builder()
                .id(entryId1).characterClass(cc).requiredLevel(3)
                .rewardType("SUBCLASS").rewardId(subRewardId1).isChoice(true).build();
        ClassLevelReward newSub = ClassLevelReward.builder()
                .id(entryId2).characterClass(cc).requiredLevel(3)
                .rewardType("SUBCLASS").rewardId(subRewardId2).isChoice(true).build();

        CharacterAcquiredReward acquiredSub = CharacterAcquiredReward.builder()
                .id(UUID.randomUUID()).character(character).classLevelReward(existingSub).build();

        when(characterRepository.findByIdForUpdate(charId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        when(thresholdService.isReadyToLevelUp(900L, 2)).thenReturn(true);
        when(classRepository.findById(classId)).thenReturn(Optional.of(cc));
        when(classLevelRepository.findByCharacterIdAndClassId(charId, classId)).thenReturn(Optional.of(ccl));
        when(rewardCatalogRepository.findAllByCharacterClassIdAndRequiredLevel(classId, 3))
                .thenReturn(List.of(existingSub, newSub));
        when(acquiredRewardRepository.findAllByCharacterId(charId)).thenReturn(List.of(acquiredSub));
        when(classLevelRepository.save(any())).thenReturn(ccl);
        when(characterRepository.save(any())).thenReturn(character);

        LevelUpRequest req = LevelUpRequest.builder()
                .classId(classId)
                .selections(List.of(LevelUpRequest.RewardSelection.builder()
                        .rewardType("SUBCLASS").rewardEntryId(entryId2).build()))
                .build();

        LevelUpResultResponse result = levelUpService.commitLevelUp(charId, "player1", req);
        assertNotNull(result);
    }

    @Test
    @DisplayName("commitLevelUp бросает 422, если выбранная награда принадлежит другому классу/уровню")
    void commitLevelUp_rewardNotBelongingToClassLevel_throws422() {
        UUID charId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        UUID otherClassId = UUID.randomUUID();
        User player = makePlayer(playerId, "player1");
        PlayerCharacter character = makeCharacter(charId, player, 1, 300);
        CharacterClass cc = CharacterClass.builder().id(classId).name("Fighter").build();
        CharacterClass otherClass = CharacterClass.builder().id(otherClassId).name("Wizard").build();
        CharacterClassLevel ccl = CharacterClassLevel.builder()
                .characterId(charId).classId(classId).classLevel(1).characterClass(cc).build();

        UUID wrongEntryId = UUID.randomUUID();
        ClassLevelReward wrongReward = ClassLevelReward.builder()
                .id(wrongEntryId).characterClass(otherClass).requiredLevel(2)
                .rewardType("FEAT").rewardId(UUID.randomUUID()).isChoice(true).build();
        ClassLevelReward correctReward = ClassLevelReward.builder()
                .id(UUID.randomUUID()).characterClass(cc).requiredLevel(2)
                .rewardType("FEAT").rewardId(UUID.randomUUID()).isChoice(true).build();

        when(characterRepository.findByIdForUpdate(charId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        when(thresholdService.isReadyToLevelUp(300L, 1)).thenReturn(true);
        when(classRepository.findById(classId)).thenReturn(Optional.of(cc));
        when(classLevelRepository.findByCharacterIdAndClassId(charId, classId)).thenReturn(Optional.of(ccl));
        when(rewardCatalogRepository.findAllByCharacterClassIdAndRequiredLevel(classId, 2))
                .thenReturn(List.of(correctReward));
        when(acquiredRewardRepository.findAllByCharacterId(charId)).thenReturn(List.of());

        LevelUpRequest req = LevelUpRequest.builder()
                .classId(classId)
                .selections(List.of(LevelUpRequest.RewardSelection.builder()
                        .rewardType("FEAT").rewardEntryId(wrongEntryId).build()))
                .build();

        assertThrows(UnprocessableEntityException.class,
                () -> levelUpService.commitLevelUp(charId, "player1", req));
    }

    @Test
    @DisplayName("commitLevelUp бросает 409 при повторном вызове до следующего набора XP")
    void commitLevelUp_calledTwice_beforeXpIncrement_throws409() {
        UUID charId = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        UUID classId = UUID.randomUUID();
        User player = makePlayer(playerId, "player1");
        PlayerCharacter character = makeCharacter(charId, player, 2, 300);

        when(characterRepository.findByIdForUpdate(charId)).thenReturn(Optional.of(character));
        when(userRepository.findByUsername("player1")).thenReturn(Optional.of(player));
        when(thresholdService.isReadyToLevelUp(300L, 2)).thenReturn(false);

        LevelUpRequest req = LevelUpRequest.builder()
                .classId(classId).selections(List.of()).build();

        assertThrows(DuplicateResourceException.class,
                () -> levelUpService.commitLevelUp(charId, "player1", req));
    }
}
