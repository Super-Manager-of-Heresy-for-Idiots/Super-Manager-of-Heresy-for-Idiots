package com.dnd.app.service;

import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.content.CharacterRewardSelection;
import com.dnd.app.domain.content.ClassLevelRewardGrant;
import com.dnd.app.domain.content.ClassLevelRewardGroup;
import com.dnd.app.domain.content.ClassLevelRewardOption;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.dto.response.CharacterRewardsResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CharacterClassLevelRepository;
import com.dnd.app.repository.CharacterRewardSelectionRepository;
import com.dnd.app.repository.ClassLevelRewardGroupRepository;
import com.dnd.app.repository.ContentCharacterClassRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Класс CharacterRewardQueryService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class CharacterRewardQueryService {

    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final CharacterClassLevelRepository classLevelRepository;
    private final CharacterRewardSelectionRepository selectionRepository;
    private final ContentCharacterClassRepository contentClassRepository;
    private final ClassLevelRewardGroupRepository rewardGroupRepository;

    /**
     * Возвращает результат операции "get character rewards" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public CharacterRewardsResponse getCharacterRewards(UUID characterId, String username) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        enforceReadAccess(character, username);

        List<CharacterClassLevel> classLevels = classLevelRepository.findAllByCharacterId(characterId);
        List<CharacterRewardSelection> selections = selectionRepository.findAllByCharacterId(characterId);

        // Seed a breakdown per class the character has levels in.
        Map<UUID, CharacterRewardsResponse.ClassBreakdown.ClassBreakdownBuilder> byClass = new LinkedHashMap<>();
        Map<UUID, Map<String, List<CharacterRewardsResponse.AcquiredReward>>> rewardsByClass = new LinkedHashMap<>();
        Map<UUID, CharacterRewardsResponse.SubclassInfo> subclassByClass = new LinkedHashMap<>();

        for (CharacterClassLevel ccl : classLevels) {
            UUID classId = ccl.getClassId();
            byClass.computeIfAbsent(classId, id -> CharacterRewardsResponse.ClassBreakdown.builder()
                    .classId(id)
                    .className(resolveClassName(id))
                    .classLevel(ccl.getClassLevel()));
            rewardsByClass.computeIfAbsent(classId, id -> new LinkedHashMap<>());
        }

        for (CharacterRewardSelection sel : selections) {
            ClassLevelRewardGroup group = sel.getRewardGroup();
            ClassLevelRewardOption option = sel.getRewardOption();
            if (group == null || option == null || group.getCharacterClass() == null) {
                continue;
            }
            ContentCharacterClass clazz = group.getCharacterClass();
            UUID classId = clazz.getId();

            byClass.computeIfAbsent(classId, id -> CharacterRewardsResponse.ClassBreakdown.builder()
                    .classId(id)
                    .className(localized(clazz.getNameRu(), clazz.getNameEn()))
                    .classLevel(null));
            Map<String, List<CharacterRewardsResponse.AcquiredReward>> rewardsByType =
                    rewardsByClass.computeIfAbsent(classId, id -> new LinkedHashMap<>());

            String optionName = localized(option.getLabelRu(), option.getLabelEn());
            CharacterRewardsResponse.AcquiredReward acquired = CharacterRewardsResponse.AcquiredReward.builder()
                    .name(optionName)
                    .acquiredAt(sel.getSelectedAt())
                    .build();

            List<ClassLevelRewardGrant> grants = option.getGrants();
            if (grants == null || grants.isEmpty()) {
                rewardsByType.computeIfAbsent("CHOICE", k -> new ArrayList<>()).add(acquired);
            } else {
                for (ClassLevelRewardGrant grant : grants) {
                    String type = grant.getGrantType() != null ? grant.getGrantType() : "CHOICE";
                    rewardsByType.computeIfAbsent(type, k -> new ArrayList<>()).add(acquired);
                    if ("SUBCLASS".equalsIgnoreCase(type)) {
                        subclassByClass.putIfAbsent(classId, CharacterRewardsResponse.SubclassInfo.builder()
                                .name(optionName)
                                .description(option.getDescription())
                                .build());
                    }
                }
            }
        }

        // AUTO (non-CHOICE) FEATURE/SUBCLASS grants are applied automatically at level-up and are
        // not recorded in character_reward_selection. Derive them on read from the class progression
        // (like spell slots are derived) so the sheet reflects auto-gained class features; this also
        // works for characters created before any persistence existed.
        for (CharacterClassLevel ccl : classLevels) {
            UUID classId = ccl.getClassId();
            int classLevel = ccl.getClassLevel();
            Map<String, List<CharacterRewardsResponse.AcquiredReward>> rewardsByType =
                    rewardsByClass.computeIfAbsent(classId, id -> new LinkedHashMap<>());
            for (ClassLevelRewardGroup group : rewardGroupRepository
                    .findAllByCharacterClassIdOrderByClassLevelAscSortOrderAsc(classId)) {
                if (group.getClassLevel() == null || group.getClassLevel() > classLevel
                        || "CHOICE".equalsIgnoreCase(group.getGroupKind()) || group.getGrants() == null) {
                    continue;
                }
                for (ClassLevelRewardGrant grant : group.getGrants()) {
                    String type = grant.getGrantType();
                    if (!"FEATURE".equalsIgnoreCase(type) && !"SUBCLASS".equalsIgnoreCase(type)) {
                        continue;
                    }
                    String name = localized(grant.getLabelRu(), grant.getLabelEn());
                    rewardsByType.computeIfAbsent(type, k -> new ArrayList<>())
                            .add(CharacterRewardsResponse.AcquiredReward.builder().name(name).build());
                    if ("SUBCLASS".equalsIgnoreCase(type)) {
                        subclassByClass.putIfAbsent(classId, CharacterRewardsResponse.SubclassInfo.builder()
                                .name(name)
                                .description(grant.getDescription())
                                .build());
                    }
                }
            }
        }

        List<CharacterRewardsResponse.ClassBreakdown> breakdown = new ArrayList<>();
        for (Map.Entry<UUID, CharacterRewardsResponse.ClassBreakdown.ClassBreakdownBuilder> entry : byClass.entrySet()) {
            breakdown.add(entry.getValue()
                    .subclass(subclassByClass.get(entry.getKey()))
                    .rewardsByType(rewardsByClass.getOrDefault(entry.getKey(), Map.of()))
                    .build());
        }

        return CharacterRewardsResponse.builder()
                .characterId(characterId)
                .totalLevel(character.getTotalLevel())
                .classBreakdown(breakdown)
                .build();
    }

    private String resolveClassName(UUID classId) {
        return contentClassRepository.findById(classId)
                .map(c -> localized(c.getNameRu(), c.getNameEn()))
                .orElse(null);
    }

    private String localized(String ru, String en) {
        if (ru != null && !ru.isBlank()) {
            return ru;
        }
        return en;
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
                if (!characterRepository.isPlayerInGameMasterCampaign(character.getOwner().getId(), user.getId())) {
                    throw new AccessDeniedException("Владелец этого персонажа не состоит ни в одной из ваших кампаний");
                }
            }
            case ADMIN -> { }
        }
    }
}
