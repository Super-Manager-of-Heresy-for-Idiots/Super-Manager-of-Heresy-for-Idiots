package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.LevelUpRequest;
import com.dnd.app.dto.response.LevelUpOptionsResponse;
import com.dnd.app.dto.response.LevelUpResultResponse;
import com.dnd.app.dto.response.RewardDetailDto;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.exception.UnprocessableEntityException;
import com.dnd.app.repository.*;
import com.dnd.app.service.reward.RewardResolverRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LevelUpService {

    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final CharacterClassRepository classRepository;
    private final CharacterClassLevelRepository classLevelRepository;
    private final ClassLevelRewardRepository rewardCatalogRepository;
    private final CharacterAcquiredRewardRepository acquiredRewardRepository;
    private final RewardResolverRegistry rewardResolverRegistry;
    private final LevelThresholdService thresholdService;

    @Transactional(readOnly = true)
    public LevelUpOptionsResponse getLevelUpOptions(UUID characterId, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        enforceReadAccess(character, username);

        if (!thresholdService.isReadyToLevelUp(character.getExperience(), character.getTotalLevel())) {
            throw new DuplicateResourceException("Character is not ready to level up");
        }

        List<CharacterClassLevel> existingLevels = classLevelRepository.findAllByCharacterId(characterId);
        Set<UUID> acquiredRewardIds = acquiredRewardRepository.findAllByCharacterId(characterId).stream()
                .map(ar -> ar.getClassLevelReward().getId())
                .collect(Collectors.toSet());

        Set<UUID> classesWithSubclass = findClassesWithAcquiredSubclass(characterId, acquiredRewardIds);

        List<CharacterClass> allClasses = classRepository.findAll();
        List<LevelUpOptionsResponse.AvailableClassOption> options = new ArrayList<>();

        for (CharacterClass cc : allClasses) {
            int currentClassLevel = existingLevels.stream()
                    .filter(cl -> cl.getClassId().equals(cc.getId()))
                    .findFirst()
                    .map(CharacterClassLevel::getClassLevel)
                    .orElse(0);

            if (currentClassLevel >= 20) continue;

            int newLevel = currentClassLevel + 1;
            List<ClassLevelReward> rewards = rewardCatalogRepository
                    .findAllByCharacterClassIdAndRequiredLevel(cc.getId(), newLevel);

            if (rewards.isEmpty() && currentClassLevel == 0) continue;

            List<LevelUpOptionsResponse.RewardGroup> groups = buildRewardGroups(
                    rewards, acquiredRewardIds, classesWithSubclass.contains(cc.getId()));

            options.add(LevelUpOptionsResponse.AvailableClassOption.builder()
                    .classId(cc.getId())
                    .className(cc.getName())
                    .currentLevelInClass(currentClassLevel)
                    .newLevelInClass(newLevel)
                    .rewardGroups(groups)
                    .build());
        }

        return LevelUpOptionsResponse.builder()
                .currentTotalLevel(character.getTotalLevel())
                .xpToNextLevel(0L)
                .availableClasses(options)
                .build();
    }

    @Transactional
    public LevelUpResultResponse commitLevelUp(UUID characterId, String username, LevelUpRequest request) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        boolean isOwner = user.getRole() == Role.PLAYER && character.getOwner().getId().equals(user.getId());
        if (!isOwner && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only the owning player can level up");
        }

        if (!thresholdService.isReadyToLevelUp(character.getExperience(), character.getTotalLevel())) {
            throw new DuplicateResourceException("Character is not ready to level up");
        }

        CharacterClass targetClass = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Character class not found"));

        Optional<CharacterClassLevel> existingLevel = classLevelRepository
                .findByCharacterIdAndClassId(characterId, targetClass.getId());
        int currentClassLevel = existingLevel.map(CharacterClassLevel::getClassLevel).orElse(0);
        int newClassLevel = currentClassLevel + 1;

        if (newClassLevel > 20) {
            throw new UnprocessableEntityException("Class level cannot exceed 20");
        }

        List<ClassLevelReward> availableRewards = rewardCatalogRepository
                .findAllByCharacterClassIdAndRequiredLevel(targetClass.getId(), newClassLevel);

        Set<UUID> acquiredRewardIds = acquiredRewardRepository.findAllByCharacterId(characterId).stream()
                .map(ar -> ar.getClassLevelReward().getId())
                .collect(Collectors.toSet());

        Map<String, List<ClassLevelReward>> rewardsByType = availableRewards.stream()
                .collect(Collectors.groupingBy(ClassLevelReward::getRewardType));

        Set<UUID> selectedEntryIds = new HashSet<>();
        if (request.getSelections() != null) {
            for (LevelUpRequest.RewardSelection sel : request.getSelections()) {
                selectedEntryIds.add(sel.getRewardEntryId());
            }
        }

        List<ClassLevelReward> toAcquire = new ArrayList<>();

        for (Map.Entry<String, List<ClassLevelReward>> entry : rewardsByType.entrySet()) {
            String type = entry.getKey();
            List<ClassLevelReward> rewards = entry.getValue();
            boolean isChoice = rewards.stream().anyMatch(r -> Boolean.TRUE.equals(r.getIsChoice()));

            if ("SUBCLASS".equals(type)) {
                boolean hasSubclass = hasAcquiredSubclassForClass(characterId, targetClass.getId(), acquiredRewardIds);
                if (hasSubclass) continue;
            }

            if (isChoice) {
                List<ClassLevelReward> chosen = rewards.stream()
                        .filter(r -> Boolean.TRUE.equals(r.getIsChoice()))
                        .filter(r -> selectedEntryIds.contains(r.getId()))
                        .toList();

                if (chosen.isEmpty()) {
                    throw new UnprocessableEntityException("Missing selection for reward type: " + type);
                }

                for (ClassLevelReward c : chosen) {
                    if (!c.getCharacterClass().getId().equals(targetClass.getId()) ||
                            !c.getRequiredLevel().equals(newClassLevel)) {
                        throw new UnprocessableEntityException(
                                "Reward entry " + c.getId() + " does not belong to this class/level");
                    }
                    if (acquiredRewardIds.contains(c.getId())) {
                        throw new UnprocessableEntityException("Reward already acquired: " + c.getId());
                    }
                    if ("SUBCLASS".equals(c.getRewardType())) {
                        boolean hasSubclass = hasAcquiredSubclassForClass(characterId, targetClass.getId(), acquiredRewardIds);
                        if (hasSubclass) {
                            throw new UnprocessableEntityException("Character already has a subclass for this class");
                        }
                    }
                    toAcquire.add(c);
                }
            }

            rewards.stream()
                    .filter(r -> Boolean.FALSE.equals(r.getIsChoice()))
                    .filter(r -> !acquiredRewardIds.contains(r.getId()))
                    .forEach(toAcquire::add);
        }

        character.setTotalLevel(character.getTotalLevel() + 1);
        characterRepository.save(character);

        if (existingLevel.isPresent()) {
            CharacterClassLevel ccl = existingLevel.get();
            ccl.setClassLevel(newClassLevel);
            classLevelRepository.save(ccl);
        } else {
            CharacterClassLevel ccl = CharacterClassLevel.builder()
                    .characterId(characterId)
                    .classId(targetClass.getId())
                    .classLevel(1)
                    .build();
            classLevelRepository.save(ccl);
        }

        List<LevelUpResultResponse.AcquiredRewardSummary> summaries = new ArrayList<>();
        for (ClassLevelReward reward : toAcquire) {
            CharacterAcquiredReward acquired = CharacterAcquiredReward.builder()
                    .character(character)
                    .classLevelReward(reward)
                    .build();
            acquiredRewardRepository.save(acquired);

            RewardDetailDto detail = rewardResolverRegistry.resolve(reward.getRewardType(), reward.getRewardId());
            summaries.add(LevelUpResultResponse.AcquiredRewardSummary.builder()
                    .rewardType(reward.getRewardType())
                    .name(detail.getName())
                    .build());
        }

        log.info("Level up committed: characterId={}, class='{}', newClassLevel={}, newTotalLevel={}, rewardsAcquired={}, user={}",
                characterId, targetClass.getName(), newClassLevel, character.getTotalLevel(), summaries.size(), username);

        return LevelUpResultResponse.builder()
                .newTotalLevel(character.getTotalLevel())
                .classLeveled(targetClass.getName())
                .newClassLevel(newClassLevel)
                .rewardsAcquired(summaries)
                .build();
    }

    private List<LevelUpOptionsResponse.RewardGroup> buildRewardGroups(
            List<ClassLevelReward> rewards,
            Set<UUID> acquiredRewardIds,
            boolean hasSubclassForThisClass) {

        Map<String, List<ClassLevelReward>> byType = rewards.stream()
                .collect(Collectors.groupingBy(ClassLevelReward::getRewardType));

        List<LevelUpOptionsResponse.RewardGroup> groups = new ArrayList<>();
        for (Map.Entry<String, List<ClassLevelReward>> entry : byType.entrySet()) {
            String type = entry.getKey();

            if ("SUBCLASS".equals(type) && hasSubclassForThisClass) continue;

            List<LevelUpOptionsResponse.RewardEntry> entries = entry.getValue().stream()
                    .map(r -> {
                        RewardDetailDto detail = rewardResolverRegistry.resolve(r.getRewardType(), r.getRewardId());
                        return LevelUpOptionsResponse.RewardEntry.builder()
                                .rewardEntryId(r.getId())
                                .rewardId(r.getRewardId())
                                .name(detail.getName())
                                .description(detail.getDescription())
                                .alreadyAcquired(acquiredRewardIds.contains(r.getId()))
                                .build();
                    })
                    .toList();

            boolean isChoice = entry.getValue().stream().anyMatch(r -> Boolean.TRUE.equals(r.getIsChoice()));

            groups.add(LevelUpOptionsResponse.RewardGroup.builder()
                    .rewardType(type)
                    .isChoice(isChoice)
                    .rewards(entries)
                    .build());
        }
        return groups;
    }

    private Set<UUID> findClassesWithAcquiredSubclass(UUID characterId, Set<UUID> acquiredRewardIds) {
        List<CharacterAcquiredReward> allAcquired = acquiredRewardRepository.findAllByCharacterId(characterId);
        return allAcquired.stream()
                .filter(ar -> "SUBCLASS".equals(ar.getClassLevelReward().getRewardType()))
                .map(ar -> ar.getClassLevelReward().getCharacterClass().getId())
                .collect(Collectors.toSet());
    }

    private boolean hasAcquiredSubclassForClass(UUID characterId, UUID classId, Set<UUID> acquiredRewardIds) {
        List<CharacterAcquiredReward> allAcquired = acquiredRewardRepository.findAllByCharacterId(characterId);
        return allAcquired.stream()
                .anyMatch(ar -> "SUBCLASS".equals(ar.getClassLevelReward().getRewardType())
                        && ar.getClassLevelReward().getCharacterClass().getId().equals(classId));
    }

    private void enforceReadAccess(PlayerCharacter character, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        switch (user.getRole()) {
            case PLAYER -> {
                if (!character.getOwner().getId().equals(user.getId())) {
                    throw new AccessDeniedException("You do not own this character");
                }
            }
            case GAME_MASTER -> {
                if (!characterRepository.isPlayerInGameMasterTeam(character.getOwner().getId(), user.getId())) {
                    throw new AccessDeniedException("This character's owner is not in any of your teams");
                }
            }
            case ADMIN -> { }
        }
    }
}
