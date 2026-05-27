package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.response.CharacterRewardsResponse;
import com.dnd.app.dto.response.RewardDetailDto;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import com.dnd.app.service.reward.RewardResolverRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CharacterRewardQueryService {

    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final CharacterClassLevelRepository classLevelRepository;
    private final CharacterAcquiredRewardRepository acquiredRewardRepository;
    private final RewardResolverRegistry rewardResolverRegistry;

    @Transactional(readOnly = true)
    public CharacterRewardsResponse getCharacterRewards(UUID characterId, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));
        enforceReadAccess(character, username);

        List<CharacterClassLevel> classLevels = classLevelRepository.findAllByCharacterId(characterId);
        List<CharacterAcquiredReward> acquiredRewards = acquiredRewardRepository.findAllByCharacterId(characterId);

        Map<UUID, List<CharacterAcquiredReward>> rewardsByClass = acquiredRewards.stream()
                .collect(Collectors.groupingBy(ar -> ar.getClassLevelReward().getCharacterClass().getId()));

        List<CharacterRewardsResponse.ClassBreakdown> breakdown = new ArrayList<>();
        for (CharacterClassLevel ccl : classLevels) {
            CharacterClass cc = ccl.getCharacterClass();
            List<CharacterAcquiredReward> classRewards = rewardsByClass.getOrDefault(cc.getId(), List.of());

            CharacterRewardsResponse.SubclassInfo subclassInfo = null;
            Map<String, List<CharacterRewardsResponse.AcquiredReward>> rewardsByType = new LinkedHashMap<>();

            for (CharacterAcquiredReward ar : classRewards) {
                ClassLevelReward clr = ar.getClassLevelReward();
                RewardDetailDto detail = rewardResolverRegistry.resolve(clr.getRewardType(), clr.getRewardId());

                if ("SUBCLASS".equals(clr.getRewardType())) {
                    subclassInfo = CharacterRewardsResponse.SubclassInfo.builder()
                            .name(detail.getName())
                            .description(detail.getDescription())
                            .build();
                }

                rewardsByType.computeIfAbsent(clr.getRewardType(), k -> new ArrayList<>())
                        .add(CharacterRewardsResponse.AcquiredReward.builder()
                                .name(detail.getName())
                                .acquiredAt(ar.getAcquiredAt())
                                .build());
            }

            breakdown.add(CharacterRewardsResponse.ClassBreakdown.builder()
                    .classId(cc.getId())
                    .className(cc.getName())
                    .classLevel(ccl.getClassLevel())
                    .subclass(subclassInfo)
                    .rewardsByType(rewardsByType)
                    .build());
        }

        return CharacterRewardsResponse.builder()
                .characterId(characterId)
                .totalLevel(character.getTotalLevel())
                .classBreakdown(breakdown)
                .build();
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
