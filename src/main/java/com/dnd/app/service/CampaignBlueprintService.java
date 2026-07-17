package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.enums.BlueprintStatus;
import com.dnd.app.domain.enums.CharacterStatus;
import com.dnd.app.domain.enums.NpcSourceType;
import com.dnd.app.domain.enums.QuestStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.ActivateHomebrewRequest;
import com.dnd.app.dto.request.CreateCampaignBlueprintRequest;
import com.dnd.app.dto.request.CreateLocationRequest;
import com.dnd.app.dto.request.CreateNpcRequest;
import com.dnd.app.dto.request.CreateQuestRequest;
import com.dnd.app.dto.request.CreateQuestRewardRequest;
import com.dnd.app.dto.request.UpdateCampaignBlueprintRequest;
import com.dnd.app.dto.request.UpdateLocationRequest;
import com.dnd.app.dto.request.UpdateNpcRequest;
import com.dnd.app.dto.request.UpdateQuestRequest;
import com.dnd.app.dto.response.CampaignBlueprintDetailResponse;
import com.dnd.app.dto.response.CampaignBlueprintResponse;
import com.dnd.app.dto.response.LocationResponse;
import com.dnd.app.dto.response.NpcResponse;
import com.dnd.app.dto.response.QuestResponse;
import com.dnd.app.dto.response.QuestRewardResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Класс CampaignBlueprintService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignBlueprintService {

    private static final String VANILLA_SLUG = "vanilla";

    private final CampaignBlueprintRepository blueprintRepository;
    private final com.dnd.app.service.media.MediaUrlResolver mediaUrlResolver;
    private final UniverseRepository universeRepository;
    private final BlueprintHomebrewRepository blueprintHomebrewRepository;
    private final BlueprintNpcRepository npcRepository;
    private final BlueprintQuestRepository questRepository;
    private final BlueprintLocationRepository locationRepository;
    private final HomebrewPackageRepository homebrewPackageRepository;
    private final UserRepository userRepository;
    private final PlayerCharacterRepository playerCharacterRepository;
    private final SpeciesRepository speciesRepository;
    private final ContentCharacterClassRepository classRepository;
    private final SpellRepository spellRepository;
    private final MonsterRepository monsterRepository;
    private final ItemTemplateRepository itemTemplateRepository;
    private final CurrencyTypeRepository currencyTypeRepository;
    private final CharacterStatRepository characterStatRepository;
    private final CharacterClassLevelRepository classLevelRepository;
    private final ItemInstanceRepository itemInstanceRepository;
    private final CharacterWalletRepository characterWalletRepository;
    private final CharacterResourceRepository characterResourceRepository;

    // ============================ Blueprint authoring ============================

    /**
     * Создает результат операции "create blueprint" в рамках бизнес-логики домена.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public CampaignBlueprintDetailResponse createBlueprint(CreateCampaignBlueprintRequest request, String username) {
        User author = getAuthoringUser(username);
        Universe universe = findUniverse(request.getUniverseId());

        CampaignBlueprint blueprint = CampaignBlueprint.builder()
                .author(author)
                .universe(universe)
                .title(request.getTitle())
                .loreDescription(request.getLoreDescription())
                .coverUrl(request.getCoverUrl())
                .allowForks(request.getAllowForks() != null ? request.getAllowForks() : true)
                .status(BlueprintStatus.DRAFT)
                .build();
        blueprint = blueprintRepository.save(blueprint);

        log.info("Campaign blueprint created: id={}, title='{}', by={}", blueprint.getId(), blueprint.getTitle(), username);
        return toDetailResponse(blueprint);
    }

    /**
     * Возвращает результат операции "get my blueprint" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public CampaignBlueprintDetailResponse getMyBlueprint(UUID id, String username) {
        User user = getUser(username);
        CampaignBlueprint blueprint = findOwnedBlueprint(id, user);
        return toDetailResponse(blueprint);
    }

    /**
     * Возвращает список для операции "list my blueprints" в рамках бизнес-логики домена.
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param pageable параметры постраничной выдачи для бизнес-списка
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public Page<CampaignBlueprintResponse> listMyBlueprints(String username, Pageable pageable) {
        User user = getUser(username);
        return blueprintRepository.findAllByAuthorIdAndDeletedAtIsNull(user.getId(), pageable)
                .map(this::toResponse);
    }

    /**
     * Обновляет результат операции "update blueprint" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public CampaignBlueprintDetailResponse updateBlueprint(UUID id, UpdateCampaignBlueprintRequest request, String username) {
        User user = getUser(username);
        CampaignBlueprint blueprint = getEditableBlueprint(id, user);

        if (request.getTitle() != null) blueprint.setTitle(request.getTitle());
        if (request.getLoreDescription() != null) blueprint.setLoreDescription(request.getLoreDescription());
        if (request.getCoverUrl() != null) blueprint.setCoverUrl(request.getCoverUrl());
        if (request.getAllowForks() != null) blueprint.setAllowForks(request.getAllowForks());
        if (request.getUniverseId() != null) blueprint.setUniverse(findUniverse(request.getUniverseId()));

        blueprint = blueprintRepository.save(blueprint);
        log.info("Campaign blueprint updated: id={}, by={}", id, username);
        return toDetailResponse(blueprint);
    }

    /**
     * Выполняет операции "soft delete blueprint" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void softDeleteBlueprint(UUID id, String username) {
        User user = getUser(username);
        CampaignBlueprint blueprint = findOwnedBlueprint(id, user);

        blueprint.setDeletedAt(Instant.now());
        blueprint.setDeletedBy(user);
        blueprintRepository.save(blueprint);
        log.info("Campaign blueprint soft-deleted: id={}, by={}", id, username);
    }

    /**
     * Публикует событие операции "publish" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public CampaignBlueprintDetailResponse publish(UUID id, String username) {
        User user = getUser(username);
        CampaignBlueprint blueprint = findOwnedBlueprint(id, user);

        if (blueprint.getStatus() != BlueprintStatus.DRAFT) {
            throw new BadRequestException("Опубликовать можно только шаблон в статусе черновика (DRAFT)");
        }
        if (blueprint.getTitle() == null || blueprint.getTitle().isBlank()) {
            throw new BadRequestException("Для публикации у шаблона должен быть непустой заголовок");
        }
        if (blueprint.getUniverse() == null) {
            throw new BadRequestException("Для публикации у шаблона должна быть выбрана вселенная");
        }
        if (isVanilla(blueprint) && blueprintHomebrewRepository.countByBlueprintId(id) > 0) {
            throw new BadRequestException("Ванильный шаблон нельзя опубликовать с подключённым homebrew-контентом");
        }

        boolean republish = blueprint.getPublishedAt() != null;
        if (!republish) {
            blueprint.setPublishedAt(Instant.now());
        } else {
            blueprint.setVersion(blueprint.getVersion() + 1);
        }
        blueprint.setStatus(BlueprintStatus.PUBLISHED);
        blueprint = blueprintRepository.save(blueprint);

        log.info("Campaign blueprint published: id={}, version={}, by={}", id, blueprint.getVersion(), username);
        return toDetailResponse(blueprint);
    }

    /**
     * Выполняет операции "unpublish" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public CampaignBlueprintDetailResponse unpublish(UUID id, String username) {
        User user = getUser(username);
        CampaignBlueprint blueprint = findOwnedBlueprint(id, user);

        if (blueprint.getStatus() != BlueprintStatus.PUBLISHED) {
            throw new BadRequestException("Снять с публикации можно только опубликованные шаблоны");
        }
        blueprint.setStatus(BlueprintStatus.DRAFT);
        blueprint = blueprintRepository.save(blueprint);
        log.info("Campaign blueprint unpublished: id={}, by={}", id, username);
        return toDetailResponse(blueprint);
    }

    // ============================ Blueprint NPCs ============================

    /**
     * Создает результат операции "create npc" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public NpcResponse createNpc(UUID blueprintId, CreateNpcRequest request, String username) {
        User user = getUser(username);
        CampaignBlueprint blueprint = getEditableBlueprint(blueprintId, user);

        BlueprintNpc npc = BlueprintNpc.builder()
                .blueprint(blueprint)
                .name(request.getName())
                .publicDescription(request.getPublicDescription())
                .privateDescription(request.getPrivateDescription())
                .isVisibleToPlayers(request.getIsVisibleToPlayers() != null ? request.getIsVisibleToPlayers() : false)
                .createdBy(user)
                .build();

        applyNpcSource(npc, blueprint, request.getSourceType(), request.getRaceId(), request.getClassId(),
                request.getLevel(), request.getAbilities(), request.getSpellIds(), request.getSourceMonsterId());

        npc = npcRepository.save(npc);
        log.info("Blueprint NPC created: id={}, blueprintId={}, by={}", npc.getId(), blueprintId, username);
        return toNpcResponse(npc);
    }

    /**
     * Обновляет результат операции "update npc" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
    /**
     * Возвращает список для операции "list npcs" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<NpcResponse> listNpcs(UUID blueprintId, String username) {
        User user = getUser(username);
        findOwnedBlueprint(blueprintId, user);
        return npcRepository.findByBlueprintId(blueprintId).stream().map(this::toNpcResponse).toList();
    }

    /**
     * Обновляет результат операции "update npc" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public NpcResponse updateNpc(UUID blueprintId, UUID npcId, UpdateNpcRequest request, String username) {
        User user = getUser(username);
        getEditableBlueprint(blueprintId, user);
        BlueprintNpc npc = npcRepository.findByIdAndBlueprintId(npcId, blueprintId)
                .orElseThrow(() -> new ResourceNotFoundException("NPC не найден в этом шаблоне"));

        if (request.getName() != null) npc.setName(request.getName());
        if (request.getPublicDescription() != null) npc.setPublicDescription(request.getPublicDescription());
        if (request.getPrivateDescription() != null) npc.setPrivateDescription(request.getPrivateDescription());
        if (request.getIsVisibleToPlayers() != null) npc.setIsVisibleToPlayers(request.getIsVisibleToPlayers());

        NpcSourceType targetType = request.getSourceType() != null ? request.getSourceType() : npc.getSourceType();
        boolean switching = request.getSourceType() != null && request.getSourceType() != npc.getSourceType();
        if (targetType == NpcSourceType.CLASS_BASED) {
            npc.setSourceType(NpcSourceType.CLASS_BASED);
            npc.setSourceMonster(null);
            if (request.getRaceId() != null) npc.setRace(resolveRace(request.getRaceId()));
            if (request.getClassId() != null) npc.setCharacterClass(resolveClass(request.getClassId()));
            if (request.getLevel() != null) npc.setLevel(request.getLevel());
            if (request.getAbilities() != null) npc.setAbilities(request.getAbilities());
            if (request.getSpellIds() != null) {
                npc.getSpells().clear();
                npc.getSpells().addAll(resolveSpells(request.getSpellIds()));
            }
            if (switching) {
                requireField(npc.getRace(), "raceId обязателен для NPC на основе класса");
                requireField(npc.getCharacterClass(), "classId обязателен для NPC на основе класса");
                requireField(npc.getLevel(), "level обязателен для NPC на основе класса");
            }
        } else if (targetType == NpcSourceType.MONSTER_BASED) {
            npc.setSourceType(NpcSourceType.MONSTER_BASED);
            if (switching) {
                npc.setRace(null);
                npc.setCharacterClass(null);
                npc.setLevel(null);
                npc.setAbilities(null);
                npc.getSpells().clear();
            }
            if (request.getSourceMonsterId() != null) {
                npc.setSourceMonster(resolveBlueprintMonster(request.getSourceMonsterId(), npc.getBlueprint()));
            }
            if (switching) {
                requireField(npc.getSourceMonster(), "sourceMonsterId обязателен для NPC на основе монстра");
            }
        }

        npc = npcRepository.save(npc);
        log.info("Blueprint NPC updated: id={}, blueprintId={}, by={}", npcId, blueprintId, username);
        return toNpcResponse(npc);
    }

    /**
     * Удаляет результат операции "delete npc" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param npcId идентификатор npc, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void deleteNpc(UUID blueprintId, UUID npcId, String username) {
        User user = getUser(username);
        getEditableBlueprint(blueprintId, user);
        BlueprintNpc npc = npcRepository.findByIdAndBlueprintId(npcId, blueprintId)
                .orElseThrow(() -> new ResourceNotFoundException("NPC не найден в этом шаблоне"));
        npcRepository.delete(npc);
        log.info("Blueprint NPC deleted: id={}, blueprintId={}, by={}", npcId, blueprintId, username);
    }

    // ============================ Blueprint quests ============================

    /**
     * Создает результат операции "create quest" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public QuestResponse createQuest(UUID blueprintId, CreateQuestRequest request, String username) {
        User user = getUser(username);
        CampaignBlueprint blueprint = getEditableBlueprint(blueprintId, user);

        BlueprintQuest quest = BlueprintQuest.builder()
                .blueprint(blueprint)
                .title(request.getTitle())
                .description(request.getDescription())
                .status(parseQuestStatus(request.getStatus()))
                .isVisibleToPlayers(request.getIsVisibleToPlayers() != null ? request.getIsVisibleToPlayers() : false)
                .createdBy(user)
                .build();
        quest = questRepository.save(quest);
        log.info("Blueprint quest created: id={}, blueprintId={}, by={}", quest.getId(), blueprintId, username);
        return toQuestResponse(quest);
    }

    /**
     * Обновляет результат операции "update quest" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
    /**
     * Возвращает список для операции "list quests" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<QuestResponse> listQuests(UUID blueprintId, String username) {
        User user = getUser(username);
        findOwnedBlueprint(blueprintId, user);
        return questRepository.findByBlueprintId(blueprintId).stream().map(this::toQuestResponse).toList();
    }

    /**
     * Обновляет результат операции "update quest" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public QuestResponse updateQuest(UUID blueprintId, UUID questId, UpdateQuestRequest request, String username) {
        User user = getUser(username);
        getEditableBlueprint(blueprintId, user);
        BlueprintQuest quest = questRepository.findByIdAndBlueprintId(questId, blueprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Квест не найден в этом шаблоне"));

        if (request.getTitle() != null) quest.setTitle(request.getTitle());
        if (request.getDescription() != null) quest.setDescription(request.getDescription());
        if (request.getIsVisibleToPlayers() != null) quest.setIsVisibleToPlayers(request.getIsVisibleToPlayers());
        if (request.getStatus() != null) quest.setStatus(parseQuestStatus(request.getStatus()));

        quest = questRepository.save(quest);
        log.info("Blueprint quest updated: id={}, blueprintId={}, by={}", questId, blueprintId, username);
        return toQuestResponse(quest);
    }

    /**
     * Удаляет результат операции "delete quest" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void deleteQuest(UUID blueprintId, UUID questId, String username) {
        User user = getUser(username);
        getEditableBlueprint(blueprintId, user);
        BlueprintQuest quest = questRepository.findByIdAndBlueprintId(questId, blueprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Квест не найден в этом шаблоне"));
        questRepository.delete(quest);
        log.info("Blueprint quest deleted: id={}, blueprintId={}, by={}", questId, blueprintId, username);
    }

    /**
     * Добавляет результат операции "add reward" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public QuestRewardResponse addReward(UUID blueprintId, UUID questId, CreateQuestRewardRequest request, String username) {
        User user = getUser(username);
        getEditableBlueprint(blueprintId, user);
        BlueprintQuest quest = questRepository.findByIdAndBlueprintId(questId, blueprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Квест не найден в этом шаблоне"));

        boolean hasItem = request.getItemTemplateId() != null;
        boolean hasCurrency = request.getCurrencyTypeId() != null;
        boolean hasXp = request.getXpAmount() != null && request.getXpAmount() > 0;
        if (!hasItem && !hasCurrency && !hasXp) {
            throw new BadRequestException("Награда должна содержать предмет, валюту или опыт");
        }

        BlueprintReward reward = BlueprintReward.builder()
                .quest(quest)
                .quantity(request.getQuantity() != null ? request.getQuantity() : 1)
                .xpAmount(request.getXpAmount())
                .build();
        if (hasItem) {
            reward.setItemTemplate(itemTemplateRepository.findById(request.getItemTemplateId())
                    .orElseThrow(() -> new ResourceNotFoundException("Шаблон предмета не найден")));
        }
        if (hasCurrency) {
            reward.setCurrencyType(currencyTypeRepository.findById(request.getCurrencyTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Тип валюты не найден")));
            reward.setCurrencyAmount(request.getCurrencyAmount());
        }
        questRepository.save(quest);
        // BlueprintReward is persisted via cascade on the quest after adding to its collection.
        quest.getRewards().add(reward);
        questRepository.saveAndFlush(quest);
        log.info("Blueprint quest reward added: questId={}, blueprintId={}, by={}", questId, blueprintId, username);
        return toRewardResponse(reward);
    }

    /**
     * Возвращает список для операции "list rewards" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<QuestRewardResponse> listRewards(UUID blueprintId, UUID questId, String username) {
        User user = getUser(username);
        findOwnedBlueprint(blueprintId, user);
        BlueprintQuest quest = questRepository.findByIdAndBlueprintId(questId, blueprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Квест не найден в этом шаблоне"));
        return quest.getRewards().stream().map(this::toRewardResponse).toList();
    }

    /**
     * Удаляет результат операции "delete reward" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param questId идентификатор quest, используемый для выбора нужного бизнес-объекта
     * @param rewardId идентификатор reward, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void deleteReward(UUID blueprintId, UUID questId, UUID rewardId, String username) {
        User user = getUser(username);
        getEditableBlueprint(blueprintId, user);
        BlueprintQuest quest = questRepository.findByIdAndBlueprintId(questId, blueprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Квест не найден в этом шаблоне"));
        boolean removed = quest.getRewards().removeIf(r -> r.getId().equals(rewardId));
        if (!removed) {
            throw new ResourceNotFoundException("Награда не найдена в этом квесте");
        }
        questRepository.saveAndFlush(quest);
        log.info("Blueprint quest reward deleted: rewardId={}, questId={}, by={}", rewardId, questId, username);
    }

    // ============================ Blueprint locations ============================

    /**
     * Создает результат операции "create location" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public LocationResponse createLocation(UUID blueprintId, CreateLocationRequest request, String username) {
        User user = getUser(username);
        CampaignBlueprint blueprint = getEditableBlueprint(blueprintId, user);

        BlueprintLocation location = BlueprintLocation.builder()
                .blueprint(blueprint)
                .name(request.getName())
                .description(request.getDescription())
                .isVisibleToPlayers(request.getIsVisibleToPlayers() != null ? request.getIsVisibleToPlayers() : false)
                .createdBy(user)
                .build();
        location = locationRepository.save(location);
        log.info("Blueprint location created: id={}, blueprintId={}, by={}", location.getId(), blueprintId, username);
        return toLocationResponse(location);
    }

    /**
     * Обновляет результат операции "update location" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
    /**
     * Возвращает список для операции "list locations" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<LocationResponse> listLocations(UUID blueprintId, String username) {
        User user = getUser(username);
        findOwnedBlueprint(blueprintId, user);
        return locationRepository.findByBlueprintId(blueprintId).stream().map(this::toLocationResponse).toList();
    }

    /**
     * Обновляет результат операции "update location" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public LocationResponse updateLocation(UUID blueprintId, UUID locationId, UpdateLocationRequest request, String username) {
        User user = getUser(username);
        getEditableBlueprint(blueprintId, user);
        BlueprintLocation location = locationRepository.findByIdAndBlueprintId(locationId, blueprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Локация не найдена в этом шаблоне"));

        if (request.getName() != null) location.setName(request.getName());
        if (request.getDescription() != null) location.setDescription(request.getDescription());
        if (request.getIsVisibleToPlayers() != null) location.setIsVisibleToPlayers(request.getIsVisibleToPlayers());

        location = locationRepository.save(location);
        log.info("Blueprint location updated: id={}, blueprintId={}, by={}", locationId, blueprintId, username);
        return toLocationResponse(location);
    }

    /**
     * Удаляет результат операции "delete location" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param locationId идентификатор location, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void deleteLocation(UUID blueprintId, UUID locationId, String username) {
        User user = getUser(username);
        getEditableBlueprint(blueprintId, user);
        BlueprintLocation location = locationRepository.findByIdAndBlueprintId(locationId, blueprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Локация не найдена в этом шаблоне"));
        locationRepository.delete(location);
        log.info("Blueprint location deleted: id={}, blueprintId={}, by={}", locationId, blueprintId, username);
    }

    // ============================ Blueprint homebrew ============================

    /**
     * Выполняет операции "attach homebrew" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void attachHomebrew(UUID blueprintId, ActivateHomebrewRequest request, String username) {
        User user = getUser(username);
        CampaignBlueprint blueprint = getEditableBlueprint(blueprintId, user);

        if (isVanilla(blueprint)) {
            throw new BadRequestException("В ванильной вселенной нельзя подключать homebrew-контент");
        }

        HomebrewPackage pkg = homebrewPackageRepository.findById(request.getHomebrewPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Homebrew-пакет не найден"));
        if (pkg.getStatus() != com.dnd.app.domain.enums.HomebrewStatus.PUBLISHED || pkg.isDeleted()) {
            throw new ResourceNotFoundException("Homebrew-пакет не найден или не опубликован");
        }
        if (blueprintHomebrewRepository.existsByBlueprintIdAndPackageId(blueprintId, pkg.getId())) {
            throw new DuplicateResourceException("Этот пакет уже подключён к шаблону");
        }

        BlueprintHomebrew link = BlueprintHomebrew.builder()
                .blueprintId(blueprintId)
                .packageId(pkg.getId())
                .build();
        blueprintHomebrewRepository.save(link);
        log.info("Homebrew attached to blueprint: package='{}', blueprintId={}, by={}", pkg.getTitle(), blueprintId, username);
    }

    /**
     * Выполняет операции "detach homebrew" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void detachHomebrew(UUID blueprintId, UUID packageId, String username) {
        User user = getUser(username);
        getEditableBlueprint(blueprintId, user);
        if (!blueprintHomebrewRepository.existsByBlueprintIdAndPackageId(blueprintId, packageId)) {
            throw new ResourceNotFoundException("Пакет не подключён к этому шаблону");
        }
        blueprintHomebrewRepository.deleteByBlueprintIdAndPackageId(blueprintId, packageId);
        log.info("Homebrew detached from blueprint: packageId={}, blueprintId={}, by={}", packageId, blueprintId, username);
    }

    // ============================ Pre-built characters ============================

    /**
     * Выполняет операции "link character" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void linkCharacter(UUID blueprintId, UUID characterId, String username) {
        User user = getUser(username);
        CampaignBlueprint blueprint = getEditableBlueprint(blueprintId, user);

        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        if (!character.getOwner().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Можно привязывать только своих персонажей");
        }
        if (character.getCampaign() != null) {
            throw new BadRequestException("Персонаж уже принадлежит кампании и не может быть пре-билдом шаблона");
        }
        if (character.getBlueprint() != null && !character.getBlueprint().getId().equals(blueprintId)) {
            throw new BadRequestException("Персонаж уже привязан к другому шаблону");
        }
        character.setBlueprint(blueprint);
        playerCharacterRepository.save(character);
        log.info("Character linked to blueprint as pre-build: characterId={}, blueprintId={}, by={}", characterId, blueprintId, username);
    }

    /**
     * Выполняет операции "unlink character" в рамках бизнес-логики домена.
     * @param blueprintId идентификатор blueprint, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void unlinkCharacter(UUID blueprintId, UUID characterId, String username) {
        User user = getUser(username);
        getEditableBlueprint(blueprintId, user);
        PlayerCharacter character = playerCharacterRepository.findById(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Персонаж не найден"));
        if (character.getBlueprint() == null || !character.getBlueprint().getId().equals(blueprintId)) {
            throw new BadRequestException("Персонаж не привязан к этому шаблону");
        }
        character.setBlueprint(null);
        playerCharacterRepository.save(character);
        log.info("Character unlinked from blueprint: characterId={}, blueprintId={}, by={}", characterId, blueprintId, username);
    }

    // ============================ Shared deep-copy of a character ============================

    /**
     * Выполняет операции "copy character" в рамках бизнес-логики домена.
     * @param original входящее значение original, используемое бизнес-сценарием
     * @param newOwner входящее значение new owner, используемое бизнес-сценарием
     * @param campaign входящее значение campaign, используемое бизнес-сценарием
     * @param blueprint входящее значение blueprint, используемое бизнес-сценарием
     * @param status входящее значение status, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public PlayerCharacter copyCharacter(PlayerCharacter original, User newOwner, Campaign campaign,
                                         CampaignBlueprint blueprint, CharacterStatus status) {
        PlayerCharacter copy = PlayerCharacter.builder()
                .name(original.getName())
                .totalLevel(original.getTotalLevel())
                .experience(original.getExperience())
                .status(status)
                .currentHp(original.getCurrentHp())
                .maxHp(original.getMaxHp())
                .race(original.getRace())
                .owner(newOwner)
                .campaign(campaign)
                .blueprint(blueprint)
                .build();
        copy = playerCharacterRepository.saveAndFlush(copy);

        for (CharacterClassLevel ccl : original.getClassLevels()) {
            CharacterClassLevel newCcl = CharacterClassLevel.builder()
                    .characterId(copy.getId())
                    .classId(ccl.getClassId())
                    .classLevel(ccl.getClassLevel())
                    .build();
            classLevelRepository.saveAndFlush(newCcl);
            copy.getClassLevels().add(newCcl);
        }
        for (CharacterStat stat : original.getStats()) {
            CharacterStat newStat = CharacterStat.builder()
                    .character(copy)
                    .statType(stat.getStatType())
                    .value(stat.getValue())
                    .build();
            characterStatRepository.save(newStat);
            copy.getStats().add(newStat);
        }
        for (ItemInstance item : itemInstanceRepository.findByOwnerCharacterId(original.getId())) {
            ItemInstance newItem = ItemInstance.builder()
                    .template(item.getTemplate())
                    .equipmentItem(item.getEquipmentItem())
                    .magicItem(item.getMagicItem())
                    .ownerCharacter(copy)
                    .customName(item.getCustomName())
                    .quantity(item.getQuantity())
                    .isUnique(item.getIsUnique())
                    .slot(item.getSlot())
                    .notes(item.getNotes())
                    .build();
            itemInstanceRepository.save(newItem);
        }
        for (CharacterWallet wallet : characterWalletRepository.findByCharacterId(original.getId())) {
            CharacterWallet newWallet = CharacterWallet.builder()
                    .character(copy)
                    .currencyType(wallet.getCurrencyType())
                    .amount(wallet.getAmount())
                    .build();
            characterWalletRepository.save(newWallet);
        }
        for (CharacterResource resource : characterResourceRepository.findByCharacterId(original.getId())) {
            CharacterResource newResource = CharacterResource.builder()
                    .character(copy)
                    .resourceType(resource.getResourceType())
                    .currentValue(resource.getCurrentValue())
                    .build();
            characterResourceRepository.save(newResource);
        }
        return copy;
    }

    // ============================ Access / lookup helpers ============================

    /**
     * Возвращает результат операции "get authoring user" в рамках бизнес-логики домена.
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    public User getAuthoringUser(String username) {
        User user = getUser(username);
        if (user.getRole() != Role.GAME_MASTER && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только мастера игры могут управлять шаблонами кампаний");
        }
        return user;
    }

    /**
     * Возвращает результат операции "get user" в рамках бизнес-логики домена.
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    public User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }

    /**
     * Находит результат операции "find blueprint" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    public CampaignBlueprint findBlueprint(UUID id) {
        return blueprintRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Шаблон кампании не найден"));
    }

    private CampaignBlueprint findOwnedBlueprint(UUID id, User user) {
        CampaignBlueprint blueprint = findBlueprint(id);
        if (blueprint.isDeleted()) {
            throw new ResourceNotFoundException("Шаблон кампании не найден");
        }
        if (!blueprint.getAuthor().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Это не ваш шаблон кампании");
        }
        return blueprint;
    }

    private CampaignBlueprint getEditableBlueprint(UUID id, User user) {
        CampaignBlueprint blueprint = findOwnedBlueprint(id, user);
        if (blueprint.getStatus() != BlueprintStatus.DRAFT) {
            throw new BadRequestException("Шаблон можно редактировать только в статусе черновика (DRAFT)");
        }
        return blueprint;
    }

    private Universe findUniverse(UUID universeId) {
        return universeRepository.findById(universeId)
                .orElseThrow(() -> new ResourceNotFoundException("Вселенная не найдена"));
    }

    private boolean isVanilla(CampaignBlueprint blueprint) {
        return blueprint.getUniverse() != null && VANILLA_SLUG.equals(blueprint.getUniverse().getSlug());
    }

    // ============================ NPC source resolution ============================

    private void applyNpcSource(BlueprintNpc npc, CampaignBlueprint blueprint, NpcSourceType sourceType,
                                UUID raceId, UUID classId, Integer level, String abilities,
                                List<UUID> spellIds, UUID sourceMonsterId) {
        npc.setSourceType(sourceType);
        if (sourceType == NpcSourceType.CLASS_BASED) {
            npc.setRace(resolveRace(requireField(raceId, "raceId обязателен для NPC на основе класса")));
            npc.setCharacterClass(resolveClass(requireField(classId, "classId обязателен для NPC на основе класса")));
            npc.setLevel(requireField(level, "level обязателен для NPC на основе класса"));
            npc.setAbilities(abilities);
            npc.getSpells().clear();
            npc.getSpells().addAll(resolveSpells(spellIds));
        } else if (sourceType == NpcSourceType.MONSTER_BASED) {
            UUID monsterId = requireField(sourceMonsterId, "sourceMonsterId обязателен для NPC на основе монстра");
            npc.setSourceMonster(resolveBlueprintMonster(monsterId, blueprint));
        }
    }

    private com.dnd.app.domain.content.Species resolveRace(UUID raceId) {
        return speciesRepository.findById(raceId)
                .orElseThrow(() -> new ResourceNotFoundException("Вид не найден"));
    }

    private ContentCharacterClass resolveClass(UUID classId) {
        return classRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Класс не найден"));
    }

    private Set<Spell> resolveSpells(List<UUID> spellIds) {
        if (spellIds == null || spellIds.isEmpty()) {
            return new LinkedHashSet<>();
        }
        Set<UUID> distinct = new LinkedHashSet<>(spellIds);
        List<Spell> found = spellRepository.findByIdIn(distinct);
        if (found.size() != distinct.size()) {
            throw new ResourceNotFoundException("Одно или несколько заклинаний не найдены");
        }
        return new LinkedHashSet<>(found);
    }

    // Allowed monster sources for a blueprint NPC: SYSTEM monsters, or monsters from a
    // homebrew package attached to this blueprint. Campaign-scoped monsters are rejected.
    private Monster resolveBlueprintMonster(UUID monsterId, CampaignBlueprint blueprint) {
        Monster monster = monsterRepository.findById(monsterId)
                .orElseThrow(() -> new ResourceNotFoundException("Монстр не найден"));
        boolean isSystem = monster.getCampaign() == null && monster.getHomebrew() == null;
        if (isSystem) {
            return monster;
        }
        if (monster.getCampaign() != null) {
            throw new BadRequestException("Монстр принадлежит кампании и не может использоваться в шаблоне");
        }
        boolean attached = blueprintHomebrewRepository.findByBlueprintId(blueprint.getId()).stream()
                .anyMatch(bh -> bh.getPackageId().equals(monster.getHomebrew().getId()));
        if (!attached) {
            throw new BadRequestException("Homebrew монстра не подключён к этому шаблону");
        }
        return monster;
    }

    private QuestStatus parseQuestStatus(String status) {
        if (status == null) {
            return QuestStatus.ACTIVE;
        }
        try {
            return QuestStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Некорректный статус квеста: " + status);
        }
    }

    private <T> T requireField(T value, String message) {
        if (value == null) {
            throw new BadRequestException(message);
        }
        return value;
    }

    // ============================ Mappers ============================

    /**
     * Преобразует данные операции "to response" в рамках бизнес-логики домена.
     * @param b входящее значение b, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public CampaignBlueprintResponse toResponse(CampaignBlueprint b) {
        return CampaignBlueprintResponse.builder()
                .id(b.getId())
                .title(b.getTitle())
                .loreDescription(b.getLoreDescription())
                .universeSlug(b.getUniverse() != null ? b.getUniverse().getSlug() : null)
                .universeName(b.getUniverse() != null ? b.getUniverse().getName() : null)
                .status(b.getStatus().name())
                .version(b.getVersion())
                .allowForks(b.getAllowForks())
                .downloadCount(b.getDownloadCount())
                .authorUsername(b.getAuthor().getUsername())
                .coverUrl(mediaUrlResolver.resolve(
                        com.dnd.app.domain.enums.MediaOwnerType.BLUEPRINT_COVER, b.getId(), b.getCoverUrl()))
                .createdAt(b.getCreatedAt())
                .publishedAt(b.getPublishedAt())
                .isDeleted(b.isDeleted())
                .parentId(b.getParent() != null ? b.getParent().getId() : null)
                .originVersion(b.getOriginVersion())
                .build();
    }

    /**
     * Преобразует данные операции "to detail response" в рамках бизнес-логики домена.
     * @param b входящее значение b, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public CampaignBlueprintDetailResponse toDetailResponse(CampaignBlueprint b) {
        List<CampaignBlueprintDetailResponse.NpcSummary> npcs = npcRepository.findByBlueprintId(b.getId()).stream()
                .map(n -> CampaignBlueprintDetailResponse.NpcSummary.builder()
                        .id(n.getId()).name(n.getName())
                        .isVisibleToPlayers(n.getIsVisibleToPlayers())
                        .sourceType(n.getSourceType() != null ? n.getSourceType().name() : null)
                        .build())
                .toList();

        List<CampaignBlueprintDetailResponse.QuestSummary> quests = questRepository.findByBlueprintId(b.getId()).stream()
                .map(q -> CampaignBlueprintDetailResponse.QuestSummary.builder()
                        .id(q.getId()).title(q.getTitle()).status(q.getStatus().name())
                        .isVisibleToPlayers(q.getIsVisibleToPlayers())
                        .rewardCount(q.getRewards().size())
                        .build())
                .toList();

        List<CampaignBlueprintDetailResponse.LocationSummary> locations = locationRepository.findByBlueprintId(b.getId()).stream()
                .map(l -> CampaignBlueprintDetailResponse.LocationSummary.builder()
                        .id(l.getId()).name(l.getName())
                        .isVisibleToPlayers(l.getIsVisibleToPlayers())
                        .build())
                .toList();

        List<CampaignBlueprintDetailResponse.HomebrewSummary> homebrew = blueprintHomebrewRepository.findByBlueprintId(b.getId()).stream()
                .map(bh -> CampaignBlueprintDetailResponse.HomebrewSummary.builder()
                        .packageId(bh.getPackageId())
                        .title(bh.getHomebrewPackage() != null ? bh.getHomebrewPackage().getTitle() : null)
                        .pinnedVersion(bh.getPinnedVersion())
                        .build())
                .toList();

        List<CampaignBlueprintDetailResponse.PreBuiltCharacterSummary> preBuilt =
                playerCharacterRepository.findByBlueprintId(b.getId()).stream()
                        .map(pc -> CampaignBlueprintDetailResponse.PreBuiltCharacterSummary.builder()
                                .id(pc.getId()).name(pc.getName()).totalLevel(pc.getTotalLevel())
                                .build())
                        .toList();

        return CampaignBlueprintDetailResponse.builder()
                .id(b.getId())
                .title(b.getTitle())
                .loreDescription(b.getLoreDescription())
                .universeSlug(b.getUniverse() != null ? b.getUniverse().getSlug() : null)
                .universeName(b.getUniverse() != null ? b.getUniverse().getName() : null)
                .status(b.getStatus().name())
                .version(b.getVersion())
                .allowForks(b.getAllowForks())
                .downloadCount(b.getDownloadCount())
                .authorUsername(b.getAuthor().getUsername())
                .coverUrl(mediaUrlResolver.resolve(
                        com.dnd.app.domain.enums.MediaOwnerType.BLUEPRINT_COVER, b.getId(), b.getCoverUrl()))
                .createdAt(b.getCreatedAt())
                .publishedAt(b.getPublishedAt())
                .isDeleted(b.isDeleted())
                .parentId(b.getParent() != null ? b.getParent().getId() : null)
                .originVersion(b.getOriginVersion())
                .npcs(npcs)
                .quests(quests)
                .locations(locations)
                .homebrew(homebrew)
                .preBuiltCharacters(preBuilt)
                .build();
    }

    private NpcResponse toNpcResponse(BlueprintNpc npc) {
        List<NpcResponse.Ref> spellRefs = npc.getSpells().isEmpty() ? null
                : npc.getSpells().stream().map(s -> ref(s.getId(), s.getNameRu())).toList();
        return NpcResponse.builder()
                .id(npc.getId())
                .name(npc.getName())
                .publicDescription(npc.getPublicDescription())
                .privateDescription(npc.getPrivateDescription())
                .isVisibleToPlayers(npc.getIsVisibleToPlayers())
                .sourceType(npc.getSourceType())
                .race(npc.getRace() == null ? null : ref(npc.getRace().getId(),
                        npc.getRace().getNameEn() != null ? npc.getRace().getNameEn() : npc.getRace().getNameRu()))
                .characterClass(npc.getCharacterClass() == null ? null
                        : ref(npc.getCharacterClass().getId(), npc.getCharacterClass().getNameRu()))
                .level(npc.getLevel())
                .abilities(npc.getAbilities())
                .spells(spellRefs)
                .sourceMonster(npc.getSourceMonster() == null ? null
                        : ref(npc.getSourceMonster().getId(), npc.getSourceMonster().getNameRusloc()))
                .createdAt(npc.getCreatedAt())
                .updatedAt(npc.getUpdatedAt())
                .build();
    }

    private NpcResponse.Ref ref(UUID id, String name) {
        return NpcResponse.Ref.builder().id(id).name(name).build();
    }

    private QuestResponse toQuestResponse(BlueprintQuest quest) {
        List<QuestRewardResponse> rewards = quest.getRewards().stream().map(this::toRewardResponse).toList();
        return QuestResponse.builder()
                .id(quest.getId())
                .title(quest.getTitle())
                .description(quest.getDescription())
                .status(quest.getStatus().name())
                .isVisibleToPlayers(quest.getIsVisibleToPlayers())
                .rewards(rewards)
                .createdAt(quest.getCreatedAt())
                .updatedAt(quest.getUpdatedAt())
                .build();
    }

    private QuestRewardResponse toRewardResponse(BlueprintReward reward) {
        return QuestRewardResponse.builder()
                .id(reward.getId())
                .itemTemplateId(reward.getItemTemplate() != null ? reward.getItemTemplate().getId() : null)
                .itemTemplateName(reward.getItemTemplate() != null ? reward.getItemTemplate().getName() : null)
                .quantity(reward.getQuantity())
                .currencyTypeId(reward.getCurrencyType() != null ? reward.getCurrencyType().getId() : null)
                .currencyTypeName(reward.getCurrencyType() != null ? reward.getCurrencyType().getNameRu() : null)
                .currencyAmount(reward.getCurrencyAmount())
                .xpAmount(reward.getXpAmount())
                .build();
    }

    private LocationResponse toLocationResponse(BlueprintLocation location) {
        return LocationResponse.builder()
                .id(location.getId())
                .name(location.getName())
                .description(location.getDescription())
                .isVisibleToPlayers(location.getIsVisibleToPlayers())
                .createdAt(location.getCreatedAt())
                .updatedAt(location.getUpdatedAt())
                .build();
    }
}
