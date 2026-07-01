package com.dnd.app.service;

import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.CharacterStat;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.CharacterRewardSelection;
import com.dnd.app.domain.content.ClassLevelRewardGroup;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.dto.content.LevelUpOptionsResponse;
import com.dnd.app.dto.content.RewardGroupDto;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.mapper.ContentClassMapper;
import com.dnd.app.repository.CampaignHomebrewRepository;
import com.dnd.app.repository.CharacterClassLevelRepository;
import com.dnd.app.repository.CharacterRewardSelectionRepository;
import com.dnd.app.repository.ClassLevelRewardGroupRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.StatTypeRepository;
import com.dnd.app.repository.UserRepository;
import com.dnd.app.util.AbilityScores;
import com.dnd.app.util.Localization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Final level-up READ model on the new content model (Phase 6). Surfaces the reward
 * groups/options/grants available for the next level of each eligible class, plus
 * derived data and already-selected state. Commit is implemented separately
 * (Phase 7); this service is read-only.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LevelUpQueryService {

    private static final int MAX_LEVEL = 20;

    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final CharacterClassLevelRepository classLevelRepository;
    private final ContentCharacterClassRepository contentClassRepository;
    private final ClassLevelRewardGroupRepository rewardGroupRepository;
    private final CharacterRewardSelectionRepository rewardSelectionRepository;
    private final CampaignHomebrewRepository campaignHomebrewRepository;
    private final StatTypeRepository statTypeRepository;
    private final ContentClassMapper classMapper;
    private final LevelThresholdService thresholdService;
    private final CampaignService campaignService;

    @Transactional(readOnly = true)
    public LevelUpOptionsResponse getLevelUpOptions(UUID characterId, String username, String lang) {
        String resolvedLang = Localization.normalize(lang);
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        enforceReadAccess(character, username);

        if (character.getTotalLevel() >= MAX_LEVEL) {
            throw new BadRequestException("Персонаж уже достиг максимального уровня");
        }
        if (!thresholdService.isReadyToLevelUp(character.getExperience(), character.getTotalLevel())) {
            throw new DuplicateResourceException("Персонаж еще не готов к повышению уровня");
        }

        List<CharacterClassLevel> existingLevels = classLevelRepository.findAllByCharacterId(characterId);
        Map<UUID, Integer> levelByClassId = new LinkedHashMap<>();
        for (CharacterClassLevel ccl : existingLevels) {
            levelByClassId.put(ccl.getClassId(), ccl.getClassLevel());
        }

        int conModifier = constitutionModifier(character);
        List<CharacterRewardSelection> selections = rewardSelectionRepository.findAllByCharacterId(characterId);

        List<LevelUpOptionsResponse.AvailableClassOption> options = new ArrayList<>();
        for (ContentCharacterClass clazz : eligibleClasses(character, levelByClassId.keySet())) {
            int currentClassLevel = levelByClassId.getOrDefault(clazz.getId(), 0);
            if (currentClassLevel >= MAX_LEVEL) {
                continue;
            }
            int newClassLevel = currentClassLevel + 1;

            List<ClassLevelRewardGroup> groups = rewardGroupRepository
                    .findAllByCharacterClassIdAndClassLevelOrderBySortOrderAsc(clazz.getId(), newClassLevel);
            List<RewardGroupDto> groupDtos = groups.stream()
                    .map(g -> classMapper.toRewardGroupDto(g, resolvedLang))
                    .toList();

            Set<UUID> groupIds = groups.stream().map(ClassLevelRewardGroup::getId).collect(java.util.stream.Collectors.toSet());
            List<LevelUpOptionsResponse.SelectedState> alreadySelected = selections.stream()
                    .filter(s -> s.getRewardGroup() != null && groupIds.contains(s.getRewardGroup().getId()))
                    .map(s -> LevelUpOptionsResponse.SelectedState.builder()
                            .rewardGroupId(s.getRewardGroup().getId())
                            .rewardOptionId(s.getRewardOption() != null ? s.getRewardOption().getId() : null)
                            .build())
                    .toList();

            options.add(LevelUpOptionsResponse.AvailableClassOption.builder()
                    .classId(clazz.getId())
                    .className(Localization.pick(resolvedLang, clazz.getNameRu(), clazz.getNameEn(),
                            clazz.getNameEn() != null ? clazz.getNameEn() : clazz.getNameRu()))
                    .currentLevelInClass(currentClassLevel)
                    .newLevelInClass(newClassLevel)
                    .hpGain(buildHpGain(clazz, conModifier))
                    .derived(buildDerived(character.getTotalLevel(), newClassLevel))
                    .rewardGroups(groupDtos)
                    .alreadySelected(alreadySelected)
                    .build());
        }

        return LevelUpOptionsResponse.builder()
                .currentTotalLevel(character.getTotalLevel())
                .xpToNextLevel(thresholdService.xpToNextLevel(character.getExperience(), character.getTotalLevel()))
                .availableClasses(options)
                .build();
    }

    /**
     * Eligible classes = the content classes the character already has a level in, plus
     * the classes available to multiclass into (core + active homebrew packages for a
     * campaign; core only for templates).
     */
    private List<ContentCharacterClass> eligibleClasses(PlayerCharacter character, Set<UUID> existingClassIds) {
        Map<UUID, ContentCharacterClass> byId = new LinkedHashMap<>();

        // already-leveled content classes (legacy ids simply won't resolve and are skipped)
        for (UUID classId : existingClassIds) {
            contentClassRepository.findById(classId).ifPresent(c -> byId.put(c.getId(), c));
        }

        for (ContentCharacterClass core : contentClassRepository.findAllByHomebrewIsNull()) {
            byId.putIfAbsent(core.getId(), core);
        }

        if (character.getCampaign() != null) {
            Set<UUID> pkgIds = campaignHomebrewRepository.findPackageIdsByCampaignId(character.getCampaign().getId());
            if (!pkgIds.isEmpty()) {
                for (ContentCharacterClass hb : contentClassRepository.findAllByHomebrewIdIn(pkgIds)) {
                    byId.putIfAbsent(hb.getId(), hb);
                }
            }
        }
        return new ArrayList<>(byId.values());
    }

    private LevelUpOptionsResponse.HpGain buildHpGain(ContentCharacterClass clazz, int conModifier) {
        int hitDie = clazz.getHitDie() != null ? clazz.getHitDie() : 8;
        int average = Math.max(1, (hitDie / 2) + 1 + conModifier);
        return LevelUpOptionsResponse.HpGain.builder()
                .hitDie(hitDie)
                .averageGain(average)
                .conModifier(conModifier)
                .build();
    }

    private LevelUpOptionsResponse.Derived buildDerived(int currentTotalLevel, int newClassLevel) {
        return LevelUpOptionsResponse.Derived.builder()
                .newTotalLevel(currentTotalLevel + 1)
                .newClassLevel(newClassLevel)
                .proficiencyBonusBefore(proficiencyBonus(currentTotalLevel))
                .proficiencyBonusAfter(proficiencyBonus(currentTotalLevel + 1))
                .build();
    }

    private int proficiencyBonus(int totalLevel) {
        int level = Math.max(1, totalLevel);
        return ((level - 1) / 4) + 2;
    }

    private int constitutionModifier(PlayerCharacter character) {
        StatType con = statTypeRepository.findAll().stream()
                .filter(st -> "Constitution".equals(st.getNameEn()))
                .findFirst().orElse(null);
        if (con == null || character.getStats() == null) {
            return 0;
        }
        return character.getStats().stream()
                .filter(s -> s.getStatType() != null && s.getStatType().getId().equals(con.getId()))
                .findFirst()
                .map(CharacterStat::getValue)
                .map(AbilityScores::modifier)
                .orElse(0);
    }

    private void enforceReadAccess(PlayerCharacter character, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        switch (user.getRole()) {
            case PLAYER -> {
                if (!character.getOwner().getId().equals(user.getId())) {
                    throw new AccessDeniedException("Этот персонаж вам не принадлежит");
                }
            }
            case GAME_MASTER -> {
                boolean isCampaignGM = character.getCampaign() != null
                        && campaignService.isGmInCampaign(character.getCampaign().getId(), user.getId());
                if (!isCampaignGM) {
                    throw new AccessDeniedException("Этот персонаж не в вашей кампании");
                }
            }
            case ADMIN -> { }
        }
    }
}
