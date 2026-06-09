package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.LevelUpRequest;
import com.dnd.app.dto.response.AbilityOptionInfo;
import com.dnd.app.dto.response.DerivedInfo;
import com.dnd.app.dto.response.HpGainInfo;
import com.dnd.app.dto.response.LevelUpOptionsResponse;
import com.dnd.app.dto.response.LevelUpResultResponse;
import com.dnd.app.dto.response.RewardDetailDto;
import com.dnd.app.dto.response.RewardDetailInfo;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
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
    private final CharacterStatRepository characterStatRepository;
    private final StatTypeRepository statTypeRepository;
    private final CampaignMemberRepository campaignMemberRepository;
    private final CampaignContentService campaignContentService;
    private final RewardResolverRegistry rewardResolverRegistry;
    private final LevelThresholdService thresholdService;
    private final CampaignService campaignService;

    @Transactional(readOnly = true)
    public LevelUpOptionsResponse getLevelUpOptions(UUID characterId, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        enforceReadAccess(character, username);

        if (character.getTotalLevel() >= 20) {
            throw new BadRequestException("Персонаж уже достиг максимального уровня");
        }

        if (!thresholdService.isReadyToLevelUp(character.getExperience(), character.getTotalLevel())) {
            throw new DuplicateResourceException("Персонаж еще не готов к повышению уровня");
        }

        List<CharacterClassLevel> existingLevels = classLevelRepository.findAllByCharacterId(characterId);
        Set<UUID> existingClassIds = existingLevels.stream()
                .map(CharacterClassLevel::getClassId)
                .collect(Collectors.toSet());

        Set<UUID> acquiredRewardIds = acquiredRewardRepository.findAllByCharacterId(characterId).stream()
                .map(ar -> ar.getClassLevelReward().getId())
                .collect(Collectors.toSet());

        Set<UUID> classesWithSubclass = findClassesWithAcquiredSubclass(characterId);

        List<CharacterClass> availableClasses = getAvailableClasses(character, existingClassIds);

        List<LevelUpOptionsResponse.AvailableClassOption> options = new ArrayList<>();

        for (CharacterClass cc : availableClasses) {
            int currentClassLevel = existingLevels.stream()
                    .filter(cl -> cl.getClassId().equals(cc.getId()))
                    .findFirst()
                    .map(CharacterClassLevel::getClassLevel)
                    .orElse(0);

            if (currentClassLevel >= 20) continue;

            int newLevel = currentClassLevel + 1;
            List<ClassLevelReward> rewards = rewardCatalogRepository
                    .findAllByCharacterClassIdAndRequiredLevel(cc.getId(), newLevel);

            List<LevelUpOptionsResponse.RewardGroup> groups = buildRewardGroups(
                    character, rewards, acquiredRewardIds, classesWithSubclass.contains(cc.getId()));

            options.add(LevelUpOptionsResponse.AvailableClassOption.builder()
                    .classId(cc.getId())
                    .className(cc.getName())
                    .currentLevelInClass(currentClassLevel)
                    .newLevelInClass(newLevel)
                    .hpGain(buildHpGain(character, cc))
                    .derived(buildDerived(character.getTotalLevel()))
                    .rewardGroups(groups)
                    .build());
        }

        long xpToNext = thresholdService.xpToNextLevel(character.getExperience(), character.getTotalLevel());

        return LevelUpOptionsResponse.builder()
                .currentTotalLevel(character.getTotalLevel())
                .xpToNextLevel(xpToNext)
                .availableClasses(options)
                .build();
    }

    @Transactional
    public LevelUpResultResponse commitLevelUp(UUID characterId, String username, LevelUpRequest request) {
        // Pessimistic write lock prevents two concurrent level-up requests from both
        // succeeding for the same XP threshold and producing a double level-up.
        PlayerCharacter character = characterRepository.findByIdForUpdate(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        enforceWriteAccess(character, user);

        if (character.getTotalLevel() >= 20) {
            throw new BadRequestException("Персонаж уже достиг максимального уровня");
        }

        if (!thresholdService.isReadyToLevelUp(character.getExperience(), character.getTotalLevel())) {
            throw new DuplicateResourceException("Персонаж еще не готов к повышению уровня");
        }

        CharacterClass targetClass = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new ResourceNotFoundException("Класс персонажа не найден"));

        Optional<CharacterClassLevel> existingLevel = classLevelRepository
                .findByCharacterIdAndClassId(characterId, targetClass.getId());
        int currentClassLevel = existingLevel.map(CharacterClassLevel::getClassLevel).orElse(0);
        int newClassLevel = currentClassLevel + 1;

        if (newClassLevel > 20) {
            throw new UnprocessableEntityException("Уровень класса не может быть выше 20");
        }

        if (currentClassLevel == 0 && character.getCampaign() != null) {
            if (!campaignContentService.isClassAvailableInCampaign(character.getCampaign().getId(), targetClass.getId())) {
                throw new BadRequestException("Этот класс недоступен в текущей кампании");
            }
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

        // Валидирует и применяет распределение очков ASI (если уровень его даёт).
        // Делается до прочих мутаций, чтобы некорректный запрос откатил транзакцию.
        List<LevelUpResultResponse.AcquiredRewardSummary> asiSummaries =
                applyAbilityScoreImprovement(character, availableRewards, request);

        List<ClassLevelReward> toAcquire = new ArrayList<>();

        for (Map.Entry<String, List<ClassLevelReward>> entry : rewardsByType.entrySet()) {
            String type = entry.getKey();
            List<ClassLevelReward> rewards = entry.getValue();
            boolean isChoice = rewards.stream().anyMatch(r -> Boolean.TRUE.equals(r.getIsChoice()));

            if ("SUBCLASS".equals(type)) {
                boolean hasSubclass = hasAcquiredSubclassForClass(characterId, targetClass.getId());
                if (hasSubclass) continue;
            }

            if (isChoice) {
                List<ClassLevelReward> chosen = rewards.stream()
                        .filter(r -> Boolean.TRUE.equals(r.getIsChoice()))
                        .filter(r -> selectedEntryIds.contains(r.getId()))
                        .toList();

                if (chosen.isEmpty()) {
                    throw new UnprocessableEntityException("Не выбран вариант для типа награды: " + type);
                }

                for (ClassLevelReward c : chosen) {
                    if (!c.getCharacterClass().getId().equals(targetClass.getId()) ||
                            !c.getRequiredLevel().equals(newClassLevel)) {
                        throw new UnprocessableEntityException(
                                "Запись награды " + c.getId() + " не относится к этому классу или уровню");
                    }
                    if (acquiredRewardIds.contains(c.getId())) {
                        throw new UnprocessableEntityException("Награда уже получена: " + c.getId());
                    }
                    if ("SUBCLASS".equals(c.getRewardType())) {
                        boolean hasSubclass = hasAcquiredSubclassForClass(characterId, targetClass.getId());
                        if (hasSubclass) {
                            throw new UnprocessableEntityException("У персонажа уже есть подкласс для этого класса");
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

        // --- Update class level ---
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

        int totalLevelBefore = character.getTotalLevel();
        character.setTotalLevel(character.getTotalLevel() + 1);

        // --- Update HP ---
        int hitDie = targetClass.getHitDie() != null ? targetClass.getHitDie() : 8;
        int conModifier = getConModifier(character);
        int hpIncrease = (hitDie / 2) + 1 + conModifier;
        if (hpIncrease < 1) hpIncrease = 1;

        if (character.getMaxHp() != null) {
            character.setMaxHp(character.getMaxHp() + hpIncrease);
            int currentHp = character.getCurrentHp() != null ? character.getCurrentHp() : 0;
            character.setCurrentHp(currentHp + hpIncrease);
        }

        // --- Update hit dice ---
        character.setHitDiceTotal(buildHitDiceTotal(characterId, targetClass, newClassLevel, existingLevel.isPresent()));

        characterRepository.save(character);

        // --- Acquire rewards ---
        List<LevelUpResultResponse.AcquiredRewardSummary> summaries = new ArrayList<>();
        for (ClassLevelReward reward : toAcquire) {
            CharacterAcquiredReward acquired = CharacterAcquiredReward.builder()
                    .character(character)
                    .classLevelReward(reward)
                    .build();
            acquiredRewardRepository.save(acquired);

            // Для ASI сводка уже сформирована applyAbilityScoreImprovement (с учётом выбора игрока).
            if ("ABILITY_SCORE_IMPROVEMENT".equals(reward.getRewardType())) {
                continue;
            }

            RewardDetailDto detail = rewardResolverRegistry.resolve(reward.getRewardType(), reward.getRewardId());
            RewardDetailInfo info = enrichDetail(character, reward, detail.getDetail());
            summaries.add(LevelUpResultResponse.AcquiredRewardSummary.builder()
                    .rewardType(reward.getRewardType())
                    .name(detail.getName())
                    .description(detail.getDescription())
                    .detail(info)
                    .build());
        }
        summaries.addAll(asiSummaries);

        log.info("Level up committed: characterId={}, class='{}', newClassLevel={}, newTotalLevel={}, hpIncrease={}, rewardsAcquired={}, user={}",
                characterId, targetClass.getName(), newClassLevel, character.getTotalLevel(), hpIncrease, summaries.size(), username);

        return LevelUpResultResponse.builder()
                .newTotalLevel(character.getTotalLevel())
                .classLeveled(targetClass.getName())
                .newClassLevel(newClassLevel)
                .hpIncrease(hpIncrease)
                .newMaxHp(character.getMaxHp())
                .proficiencyBonusBefore(proficiencyBonus(totalLevelBefore))
                .proficiencyBonusAfter(proficiencyBonus(character.getTotalLevel()))
                .rewardsAcquired(summaries)
                .build();
    }

    private List<CharacterClass> getAvailableClasses(PlayerCharacter character, Set<UUID> existingClassIds) {
        if (character.getCampaign() == null) {
            List<CharacterClass> vanilla = classRepository.findAllByHomebrewIsNull();
            if (!existingClassIds.isEmpty()) {
                Set<UUID> vanillaIds = vanilla.stream().map(CharacterClass::getId).collect(Collectors.toSet());
                List<CharacterClass> homebrewOwned = existingClassIds.stream()
                        .filter(id -> !vanillaIds.contains(id))
                        .map(id -> classRepository.findById(id).orElse(null))
                        .filter(Objects::nonNull)
                        .toList();
                List<CharacterClass> result = new ArrayList<>(vanilla);
                result.addAll(homebrewOwned);
                return result;
            }
            return vanilla;
        }

        List<CharacterClass> allClasses = classRepository.findAll();
        return allClasses.stream()
                .filter(cc -> {
                    if (existingClassIds.contains(cc.getId())) return true;
                    if (cc.getHomebrew() == null) return true;
                    return campaignContentService.isClassAvailableInCampaign(
                            character.getCampaign().getId(), cc.getId());
                })
                .toList();
    }

    private int getConModifier(PlayerCharacter character) {
        Optional<StatType> conStat = statTypeRepository.findAll().stream()
                .filter(st -> "Constitution".equals(st.getName()))
                .findFirst();
        if (conStat.isEmpty()) return 0;

        Optional<CharacterStat> stat = character.getStats().stream()
                .filter(s -> s.getStatType().getId().equals(conStat.get().getId()))
                .findFirst();
        if (stat.isEmpty()) return 0;

        return (stat.get().getValue() - 10) / 2;
    }

    /** Бонус мастерства по правилам системы как функция суммарного уровня. */
    private int proficiencyBonus(int totalLevel) {
        int level = Math.max(1, totalLevel);
        return ((level - 1) / 4) + 2;
    }

    private HpGainInfo buildHpGain(PlayerCharacter character, CharacterClass cc) {
        int hitDie = cc.getHitDie() != null ? cc.getHitDie() : 8;
        int conMod = getConModifier(character);
        int average = (hitDie / 2) + 1 + conMod;
        if (average < 1) average = 1;
        int rolledMin = Math.max(1, 1 + conMod);
        int rolledMax = hitDie + conMod;
        return HpGainInfo.builder()
                .hitDie(hitDie)
                .conModifier(conMod)
                .average(average)
                .rolledMin(rolledMin)
                .rolledMax(rolledMax)
                .currentMaxHp(character.getMaxHp())
                .build();
    }

    private DerivedInfo buildDerived(int currentTotalLevel) {
        // spellSlotsGained / cantripsGained пока не считаются (нет таблиц прогрессии в БД).
        return DerivedInfo.builder()
                .proficiencyBonusBefore(proficiencyBonus(currentTotalLevel))
                .proficiencyBonusAfter(proficiencyBonus(currentTotalLevel + 1))
                .build();
    }

    /**
     * Для ASI достраивает список характеристик персонажа как варианты выбора
     * (резолвер не имеет доступа к персонажу). Остальные типы возвращаются как есть.
     */
    private RewardDetailInfo enrichDetail(PlayerCharacter character, ClassLevelReward reward, RewardDetailInfo detail) {
        if (detail != null && "ABILITY_SCORE_IMPROVEMENT".equals(reward.getRewardType())
                && character.getStats() != null) {
            List<AbilityOptionInfo> options = character.getStats().stream()
                    .map(s -> AbilityOptionInfo.builder()
                            .statTypeId(s.getStatType().getId())
                            .name(s.getStatType().getName())
                            .currentScore(s.getValue())
                            .maxScore(20)
                            .build())
                    .toList();
            detail.setAbilityOptions(options);
        }
        return detail;
    }

    /**
     * Проверяет и применяет распределение очков ASI по правилам PHB:
     * +2 одной характеристике или +1 двум разным, итог не выше 20.
     * ASI обязателен на уровнях, где он есть в каталоге, и не может быть пропущен.
     * Возвращает сводки по применённым повышениям (для rewardsAcquired).
     */
    private List<LevelUpResultResponse.AcquiredRewardSummary> applyAbilityScoreImprovement(
            PlayerCharacter character, List<ClassLevelReward> availableRewards, LevelUpRequest request) {

        boolean asiGranted = availableRewards.stream()
                .anyMatch(r -> "ABILITY_SCORE_IMPROVEMENT".equals(r.getRewardType()));

        LevelUpRequest.AbilityScoreImprovement asi = request.getAbilityScoreImprovement();

        if (!asiGranted) {
            if (asi != null) {
                throw new UnprocessableEntityException("Повышение характеристик не предусмотрено на этом уровне");
            }
            return List.of();
        }

        if (asi == null || asi.getIncreases() == null || asi.getIncreases().isEmpty()) {
            throw new UnprocessableEntityException("Необходимо распределить очки повышения характеристик");
        }

        List<LevelUpRequest.StatIncrease> increases = asi.getIncreases();
        int total = increases.stream().mapToInt(LevelUpRequest.StatIncrease::getAmount).sum();
        if (total != 2) {
            throw new UnprocessableEntityException(
                    "Нужно распределить ровно 2 очка: +2 одной характеристике или +1 двум разным");
        }
        if (increases.size() == 1 && increases.get(0).getAmount() != 2) {
            throw new UnprocessableEntityException("При выборе одной характеристики повышение должно быть на 2");
        }
        if (increases.size() == 2 &&
                !(increases.get(0).getAmount() == 1 && increases.get(1).getAmount() == 1)) {
            throw new UnprocessableEntityException("При выборе двух характеристик каждая повышается на 1");
        }
        long distinct = increases.stream()
                .map(LevelUpRequest.StatIncrease::getStatTypeId)
                .distinct().count();
        if (distinct != increases.size()) {
            throw new UnprocessableEntityException("Характеристики для повышения должны быть разными");
        }

        List<LevelUpResultResponse.AcquiredRewardSummary> summaries = new ArrayList<>();
        for (LevelUpRequest.StatIncrease inc : increases) {
            CharacterStat stat = character.getStats().stream()
                    .filter(s -> s.getStatType().getId().equals(inc.getStatTypeId()))
                    .findFirst()
                    .orElseThrow(() -> new UnprocessableEntityException(
                            "Характеристика не принадлежит персонажу: " + inc.getStatTypeId()));

            int before = stat.getValue();
            int after = before + inc.getAmount();
            if (after > 20) {
                throw new UnprocessableEntityException(
                        "Значение характеристики '" + stat.getStatType().getName() + "' не может превысить 20");
            }
            stat.setValue(after);
            characterStatRepository.save(stat);

            RewardDetailInfo detail = RewardDetailInfo.builder()
                    .abilityStatName(stat.getStatType().getName())
                    .currentScore(after)
                    .maxScore(20)
                    .build();
            summaries.add(LevelUpResultResponse.AcquiredRewardSummary.builder()
                    .rewardType("ABILITY_SCORE_IMPROVEMENT")
                    .name("+" + inc.getAmount() + " " + stat.getStatType().getName())
                    .description(stat.getStatType().getName() + ": " + before + " → " + after)
                    .detail(detail)
                    .build());
        }
        return summaries;
    }

    private String buildHitDiceTotal(UUID characterId, CharacterClass leveledClass, int newClassLevel, boolean existedBefore) {
        List<CharacterClassLevel> allLevels = classLevelRepository.findAllByCharacterId(characterId);

        Map<UUID, Integer> levelsByClassId = new LinkedHashMap<>();
        for (CharacterClassLevel ccl : allLevels) {
            if (ccl.getClassId().equals(leveledClass.getId())) {
                levelsByClassId.put(ccl.getClassId(), newClassLevel);
            } else {
                levelsByClassId.put(ccl.getClassId(), ccl.getClassLevel());
            }
        }
        if (!existedBefore) {
            levelsByClassId.put(leveledClass.getId(), 1);
        }

        List<String> parts = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : levelsByClassId.entrySet()) {
            CharacterClass cc = classRepository.findById(entry.getKey()).orElse(null);
            int hitDie = (cc != null && cc.getHitDie() != null) ? cc.getHitDie() : 8;
            parts.add(entry.getValue() + "d" + hitDie);
        }

        return String.join(" + ", parts);
    }

    private List<LevelUpOptionsResponse.RewardGroup> buildRewardGroups(
            PlayerCharacter character,
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
                                .detail(enrichDetail(character, r, detail.getDetail()))
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

    private Set<UUID> findClassesWithAcquiredSubclass(UUID characterId) {
        List<CharacterAcquiredReward> allAcquired = acquiredRewardRepository.findAllByCharacterId(characterId);
        return allAcquired.stream()
                .filter(ar -> "SUBCLASS".equals(ar.getClassLevelReward().getRewardType()))
                .map(ar -> ar.getClassLevelReward().getCharacterClass().getId())
                .collect(Collectors.toSet());
    }

    private boolean hasAcquiredSubclassForClass(UUID characterId, UUID classId) {
        List<CharacterAcquiredReward> allAcquired = acquiredRewardRepository.findAllByCharacterId(characterId);
        return allAcquired.stream()
                .anyMatch(ar -> "SUBCLASS".equals(ar.getClassLevelReward().getRewardType())
                        && ar.getClassLevelReward().getCharacterClass().getId().equals(classId));
    }

    private void enforceWriteAccess(PlayerCharacter character, User user) {
        boolean isOwner = character.getOwner().getId().equals(user.getId());
        boolean isCampaignGM = character.getCampaign() != null
                && campaignService.isGmInCampaign(character.getCampaign().getId(), user.getId());
        if (!isOwner && !isCampaignGM && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Нет прав на повышение уровня этого персонажа");
        }
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
