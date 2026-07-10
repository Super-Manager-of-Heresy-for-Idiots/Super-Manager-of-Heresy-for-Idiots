package com.dnd.app.service;

import com.dnd.app.domain.CharacterClassLevel;
import com.dnd.app.domain.CharacterSpellSlotUsage;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.response.SpellSlotsResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.exception.UnprocessableEntityException;
import com.dnd.app.repository.CharacterClassLevelRepository;
import com.dnd.app.repository.CharacterSpellSlotUsageRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Класс SpellSlotService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpellSlotService {

    private static final String PACT_COLUMN = "yacheyki-zaklinaniy";
    private static final String SLOT_COLUMN_PREFIX = "yacheyki-zaklinaniy-level";
    private static final int MAX_SPELL_LEVEL = 9;

    /**
     * Reserved shared-pool key format for referencing a slot level from the feature-rules resource
     * layer (S2 decision): a resource definition with {@code shared_pool_key = "spell_slot_L{n}"} means
     * "draws from the level-n spell slots of this service". Slot state itself deliberately STAYS in
     * {@code character_spell_slot_usage} (single source of truth); modelling the nine pools as Stage-5
     * resources is optional later work and must go through this key format.
     */
    public static final String SHARED_POOL_KEY_FORMAT = "spell_slot_L%d";

    /**
     * Выполняет операции "shared pool key" в рамках бизнес-логики домена.
     * @param spellLevel входящее значение spell level, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static String sharedPoolKey(int spellLevel) {
        return String.format(SHARED_POOL_KEY_FORMAT, spellLevel);
    }

    private final JdbcTemplate jdbc;
    private final PlayerCharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final CharacterClassLevelRepository classLevelRepository;
    private final CharacterSpellSlotUsageRepository usageRepository;
    private final CampaignService campaignService;

    /**
     * Возвращает результат операции "get slots" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public SpellSlotsResponse getSlots(UUID characterId, String username) {
        loadAndAuthorize(characterId, username, false);
        return buildResponse(characterId);
    }

    /**
     * Выполняет операции "expend" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param spellLevel входящее значение spell level, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public SpellSlotsResponse expend(UUID characterId, String username, int spellLevel) {
        loadAndAuthorize(characterId, username, true);
        expendCore(characterId, spellLevel);
        return buildResponse(characterId);
    }

    /**
     * Выполняет операции "expend internal" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param spellLevel входящее значение spell level, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public SpellSlotsResponse expendInternal(UUID characterId, int spellLevel) {
        expendCore(characterId, spellLevel);
        return buildResponse(characterId);
    }

    private void expendCore(UUID characterId, int spellLevel) {
        if (spellLevel < 1 || spellLevel > MAX_SPELL_LEVEL) {
            throw new BadRequestException("Уровень ячейки должен быть от 1 до 9");
        }
        int max = deriveMaxSlots(characterId).getOrDefault(spellLevel, 0);
        if (max <= 0) {
            throw new UnprocessableEntityException("У персонажа нет ячеек заклинаний " + spellLevel + "-го уровня");
        }
        CharacterSpellSlotUsage usage = usageRepository
                .findByCharacterIdAndSpellLevel(characterId, spellLevel)
                .orElseGet(() -> CharacterSpellSlotUsage.builder()
                        .characterId(characterId).spellLevel(spellLevel).expendedCount(0).build());
        if (usage.getExpendedCount() >= max) {
            throw new UnprocessableEntityException(
                    "Все ячейки " + spellLevel + "-го уровня уже потрачены");
        }
        usage.setExpendedCount(usage.getExpendedCount() + 1);
        usageRepository.save(usage);
    }

    /**
     * Выполняет операции "restore all" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public SpellSlotsResponse restoreAll(UUID characterId, String username) {
        loadAndAuthorize(characterId, username, true);
        for (CharacterSpellSlotUsage usage : usageRepository.findAllByCharacterId(characterId)) {
            if (usage.getExpendedCount() != 0) {
                usage.setExpendedCount(0);
                usageRepository.save(usage);
            }
        }
        return buildResponse(characterId);
    }

    /**
     * Выполняет операции "restore one" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param spellLevel входящее значение spell level, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public SpellSlotsResponse restoreOne(UUID characterId, String username, int spellLevel) {
        loadAndAuthorize(characterId, username, true);
        if (spellLevel < 1 || spellLevel > MAX_SPELL_LEVEL) {
            throw new BadRequestException("Уровень ячейки должен быть от 1 до 9");
        }
        usageRepository.findByCharacterIdAndSpellLevel(characterId, spellLevel).ifPresent(usage -> {
            if (usage.getExpendedCount() > 0) {
                usage.setExpendedCount(usage.getExpendedCount() - 1);
                usageRepository.save(usage);
            }
        });
        return buildResponse(characterId);
    }

    /**
     * Выполняет операции "restore half" в рамках бизнес-логики домена.
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public SpellSlotsResponse restoreHalf(UUID characterId, String username) {
        loadAndAuthorize(characterId, username, true);
        for (CharacterSpellSlotUsage usage : usageRepository.findAllByCharacterId(characterId)) {
            int restored = usage.getExpendedCount() / 2;
            if (restored > 0) {
                usage.setExpendedCount(usage.getExpendedCount() - restored);
                usageRepository.save(usage);
            }
        }
        return buildResponse(characterId);
    }

    // ------------------------------------------------------------------ internals

    private SpellSlotsResponse buildResponse(UUID characterId) {
        Map<Integer, Integer> max = deriveMaxSlots(characterId);
        Map<Integer, Integer> expended = new HashMap<>();
        for (CharacterSpellSlotUsage usage : usageRepository.findAllByCharacterId(characterId)) {
            expended.put(usage.getSpellLevel(), usage.getExpendedCount());
        }
        List<SpellSlotsResponse.SlotLevel> levels = new ArrayList<>();
        for (int level = 1; level <= MAX_SPELL_LEVEL; level++) {
            int m = max.getOrDefault(level, 0);
            int e = expended.getOrDefault(level, 0);
            if (m <= 0 && e <= 0) {
                continue;
            }
            levels.add(SpellSlotsResponse.SlotLevel.builder()
                    .spellLevel(level)
                    .max(m)
                    .expended(e)
                    .available(Math.max(0, m - e))
                    .build());
        }
        return SpellSlotsResponse.builder().levels(levels).build();
    }

    /** Derives spell-level -> max slot count by reading class progression for each class. */
    private Map<Integer, Integer> deriveMaxSlots(UUID characterId) {
        Map<Integer, Integer> byLevel = new HashMap<>();
        for (CharacterClassLevel ccl : classLevelRepository.findAllByCharacterId(characterId)) {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT cpc.slug AS slug, cpv.value_numeric AS num "
                            + "FROM class_progression_value cpv "
                            + "JOIN class_progression_column cpc "
                            + "  ON cpc.class_progression_column_id = cpv.class_progression_column_id "
                            + "WHERE cpv.class_id = ? AND cpv.class_level = ? "
                            + "  AND cpc.slug LIKE 'yacheyki-zaklinaniy%'",
                    ccl.getClassId(), ccl.getClassLevel());
            for (Map<String, Object> row : rows) {
                Object num = row.get("num");
                if (num == null) {
                    continue;
                }
                int count = ((Number) num).intValue();
                if (count <= 0) {
                    continue;
                }
                Integer spellLevel = slotLevelFromSlug((String) row.get("slug"), ccl.getClassLevel());
                if (spellLevel != null) {
                    byLevel.merge(spellLevel, count, Integer::sum);
                }
            }
        }
        return byLevel;
    }

    private Integer slotLevelFromSlug(String slug, int classLevel) {
        if (slug == null) {
            return null;
        }
        if (slug.equals(PACT_COLUMN)) {
            // Warlock Pact Magic: a single slot pool whose level scales with warlock level.
            if (classLevel >= 9) {
                return 5;
            }
            if (classLevel >= 7) {
                return 4;
            }
            if (classLevel >= 5) {
                return 3;
            }
            if (classLevel >= 3) {
                return 2;
            }
            return 1;
        }
        if (slug.startsWith(SLOT_COLUMN_PREFIX)) {
            try {
                int level = Integer.parseInt(slug.substring(SLOT_COLUMN_PREFIX.length()));
                return level >= 1 && level <= MAX_SPELL_LEVEL ? level : null;
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private void loadAndAuthorize(UUID characterId, String username, boolean write) {
        PlayerCharacter character = characterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        boolean isOwner = character.getOwner() != null && character.getOwner().getId().equals(user.getId());
        boolean isGM = character.getCampaign() != null
                && campaignService.isGmInCampaign(character.getCampaign().getId(), user.getId());
        if (!isOwner && !isGM && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException(write
                    ? "Нет прав на изменение этого персонажа"
                    : "Нет доступа к этому персонажу");
        }
    }
}
