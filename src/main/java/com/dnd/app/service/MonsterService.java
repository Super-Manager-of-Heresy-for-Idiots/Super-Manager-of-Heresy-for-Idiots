package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.*;
import com.dnd.app.dto.request.MonsterRequest;
import com.dnd.app.dto.response.MonsterResponse;
import com.dnd.app.dto.response.MonsterSummaryResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import com.dnd.app.util.Localization;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Класс MonsterService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonsterService {

    private final MonsterRepository monsterRepository;
    private final AlignmentRepository alignmentRepository;
    private final CreatureTypeRepository creatureTypeRepository;
    private final LanguageRepository languageRepository;
    private final SenseTypeRepository senseTypeRepository;
    private final MovementTypeRepository movementTypeRepository;
    private final HabitatRepository habitatRepository;
    private final TreasureTagRepository treasureTagRepository;
    private final BestiaryConditionRepository bestiaryConditionRepository;
    private final MonsterGearItemRepository monsterGearItemRepository;
    private final SourceRepository sourceRepository;
    private final BestiarySizeRepository bestiarySizeRepository;
    private final BestiaryAbilityRepository bestiaryAbilityRepository;
    private final DamageTypeRepository damageTypeRepository;
    private final ProficiencySkillRepository proficiencySkillRepository;
    private final UserRepository userRepository;
    private final HomebrewPackageRepository homebrewPackageRepository;
    private final HomebrewContentItemRepository contentItemRepository;
    private final CampaignHomebrewRepository campaignHomebrewRepository;
    private final CampaignService campaignService;
    private final WebSocketEventService webSocketEventService;
    private final com.dnd.app.service.homebrew.HomebrewAccessService homebrewAccessService;

    // =================================== Reads ===================================

    /**
     * Возвращает список для операции "list system monsters" в рамках бизнес-логики домена.
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<MonsterSummaryResponse> listSystemMonsters(String username) {
        return listSystemMonsters(username, Localization.DEFAULT_LANG);
    }

    /**
     * Возвращает список для операции "list system monsters" в рамках бизнес-логики домена.
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<MonsterSummaryResponse> listSystemMonsters(String username, String lang) {
        User user = getUser(username);
        List<Monster> list = user.getRole() == Role.ADMIN
                ? monsterRepository.findAllByCampaignIsNullAndHomebrewIsNull()
                : monsterRepository.findAllByCampaignIsNullAndHomebrewIsNullAndIsActiveTrue();
        return list.stream().map(m -> toSummary(m, lang)).toList();
    }

    /**
     * Возвращает список для операции "list homebrew monsters" в рамках бизнес-логики домена.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<MonsterSummaryResponse> listHomebrewMonsters(UUID packageId, String username) {
        return listHomebrewMonsters(packageId, username, Localization.DEFAULT_LANG);
    }

    /**
     * Возвращает список для операции "list homebrew monsters" в рамках бизнес-логики домена.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<MonsterSummaryResponse> listHomebrewMonsters(UUID packageId, String username, String lang) {
        // SEC-1 / P0-1: раньше проверялось только существование пользователя, из-за чего любой
        // аутентифицированный пользователь мог прочитать бестиарий (включая черновики) чужого пакета.
        // Теперь доступ к пакету проходит через единый guard: чужой пакет виден только если PUBLISHED.
        homebrewAccessService.enforceReadable(packageId, getUser(username));
        return monsterRepository.findAllByHomebrewId(packageId).stream().map(m -> toSummary(m, lang)).toList();
    }

    /**
     * Возвращает список для операции "list campaign monsters" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<MonsterSummaryResponse> listCampaignMonsters(UUID campaignId, String username) {
        return listCampaignMonsters(campaignId, username, Localization.DEFAULT_LANG);
    }

    /**
     * Возвращает список для операции "list campaign monsters" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<MonsterSummaryResponse> listCampaignMonsters(UUID campaignId, String username, String lang) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceMembershipOrAdmin(campaign, user);
        boolean gm = isGmOrAdmin(campaignId, user);
        List<Monster> list = gm
                ? monsterRepository.findAllByCampaignId(campaignId)
                : monsterRepository.findAllByCampaignIdAndIsVisibleToPlayersTrue(campaignId);
        return list.stream().map(m -> toSummary(m, lang)).toList();
    }

    /**
     * Возвращает результат операции "get monster" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public MonsterResponse getMonster(UUID id, String username) {
        return getMonster(id, username, Localization.DEFAULT_LANG);
    }

    /**
     * Возвращает результат операции "get monster" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public MonsterResponse getMonster(UUID id, String username, String lang) {
        User user = getUser(username);
        Monster monster = findMonster(id);
        enforceCanRead(monster, user);
        return toResponse(monster, lang);
    }

    /**
     * Возвращает результат операции "get usable campaign monster" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param monsterId идентификатор monster, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public Monster getUsableCampaignMonster(UUID campaignId, UUID monsterId, String username) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        Monster monster = findMonster(monsterId);
        enforceCanUseAsCampaignSource(monster, campaign, user);
        return monster;
    }

    // ============================= ADMIN system CRUD =============================

    /**
     * Создает результат операции "create system monster" в рамках бизнес-логики домена.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
    /**
     * Создает результат операции "create system monster" в рамках бизнес-логики домена.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse createSystemMonster(MonsterRequest request, String username) {
        return createSystemMonster(request, username, Localization.DEFAULT_LANG);
    }

    /**
     * Создает результат операции "create system monster" в рамках бизнес-логики домена.
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse createSystemMonster(MonsterRequest request, String username, String lang) {
        User admin = requireRole(username, Role.ADMIN);
        Monster monster = new Monster();
        applyRequest(monster, request, admin, null, null);
        Monster saved = monsterRepository.save(monster);
        log.info("System monster created: id={}, slug='{}', by={}", saved.getId(), saved.getSlug(), username);
        return toResponse(saved, lang);
    }

    /**
     * Обновляет результат операции "update system monster" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse updateSystemMonster(UUID id, MonsterRequest request, String username) {
        return updateSystemMonster(id, request, username, Localization.DEFAULT_LANG);
    }

    /**
     * Обновляет результат операции "update system monster" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse updateSystemMonster(UUID id, MonsterRequest request, String username, String lang) {
        User admin = requireRole(username, Role.ADMIN);
        Monster monster = findMonster(id);
        requireScope(monster, "SYSTEM");
        applyRequest(monster, request, admin, null, null);
        return toResponse(monsterRepository.save(monster), lang);
    }

    /**
     * Устанавливает результат операции "set system monster active" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param active входящее значение active, используемое бизнес-сценарием
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse setSystemMonsterActive(UUID id, boolean active, String username) {
        return setSystemMonsterActive(id, active, username, Localization.DEFAULT_LANG);
    }

    /**
     * Устанавливает результат операции "set system monster active" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param active входящее значение active, используемое бизнес-сценарием
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse setSystemMonsterActive(UUID id, boolean active, String username, String lang) {
        User admin = requireRole(username, Role.ADMIN);
        Monster monster = findMonster(id);
        requireScope(monster, "SYSTEM");
        monster.setIsActive(active);
        monster.setUpdatedBy(admin);
        return toResponse(monsterRepository.save(monster), lang);
    }

    /**
     * Удаляет результат операции "delete system monster" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void deleteSystemMonster(UUID id, String username) {
        requireRole(username, Role.ADMIN);
        Monster monster = findMonster(id);
        requireScope(monster, "SYSTEM");
        monsterRepository.delete(monster);
        log.info("System monster deleted: id={}, by={}", id, username);
    }

    // ============================ GM homebrew CRUD ============================

    /**
     * Создает результат операции "create homebrew monster" в рамках бизнес-логики домена.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse createHomebrewMonster(UUID packageId, MonsterRequest request, String username) {
        return createHomebrewMonster(packageId, request, username, Localization.DEFAULT_LANG);
    }

    /**
     * Создает результат операции "create homebrew monster" в рамках бизнес-логики домена.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse createHomebrewMonster(UUID packageId, MonsterRequest request, String username, String lang) {
        User gm = requireGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);
        Monster monster = new Monster();
        applyRequest(monster, request, gm, pkg, null);
        Monster saved = monsterRepository.save(monster);
        attachToPackage(pkg, saved.getId());
        log.info("Homebrew monster created: packageId={}, id={}, by={}", packageId, saved.getId(), username);
        return toResponse(saved, lang);
    }

    /**
     * Обновляет результат операции "update homebrew monster" в рамках бизнес-логики домена.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse updateHomebrewMonster(UUID packageId, UUID id, MonsterRequest request, String username) {
        return updateHomebrewMonster(packageId, id, request, username, Localization.DEFAULT_LANG);
    }

    /**
     * Обновляет результат операции "update homebrew monster" в рамках бизнес-логики домена.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse updateHomebrewMonster(UUID packageId, UUID id, MonsterRequest request, String username, String lang) {
        User gm = requireGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);
        Monster monster = findMonster(id);
        requireSameHomebrew(monster, pkg);
        applyRequest(monster, request, gm, pkg, null);
        return toResponse(monsterRepository.save(monster), lang);
    }

    /**
     * Удаляет результат операции "delete homebrew monster" в рамках бизнес-логики домена.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void deleteHomebrewMonster(UUID packageId, UUID id, String username) {
        User gm = requireGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);
        Monster monster = findMonster(id);
        requireSameHomebrew(monster, pkg);
        contentItemRepository.findAllByHomebrewPackageId(pkg.getId()).stream()
                .filter(ci -> ci.getContentType() == ContentType.MONSTER && ci.getContentId().equals(id))
                .forEach(contentItemRepository::delete);
        monsterRepository.delete(monster);
        log.info("Homebrew monster deleted: packageId={}, id={}, by={}", packageId, id, username);
    }

    /**
     * Выполняет операции "duplicate monster into homebrew" в рамках бизнес-логики домена.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param sourceId идентификатор source, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse duplicateMonsterIntoHomebrew(UUID packageId, UUID sourceId, String username) {
        return duplicateMonsterIntoHomebrew(packageId, sourceId, username, Localization.DEFAULT_LANG);
    }

    /**
     * Выполняет операции "duplicate monster into homebrew" в рамках бизнес-логики домена.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param sourceId идентификатор source, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse duplicateMonsterIntoHomebrew(UUID packageId, UUID sourceId, String username, String lang) {
        User gm = requireGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);
        Monster source = findMonster(sourceId);
        if (!"SYSTEM".equals(scopeOf(source)) && !"HOMEBREW".equals(scopeOf(source))) {
            throw new BadRequestException("Only system or homebrew monsters can be duplicated into homebrew");
        }
        Monster copy = cloneMonster(source, gm);
        copy.setHomebrew(pkg);
        copy.setCampaign(null);
        Monster saved = monsterRepository.save(copy);
        attachToPackage(pkg, saved.getId());
        log.info("Monster duplicated into homebrew: sourceId={}, newId={}, packageId={}, by={}",
                sourceId, saved.getId(), packageId, username);
        return toResponse(saved, lang);
    }

    // ========================= Campaign (master) CRUD =========================

    /**
     * Создает результат операции "create campaign monster" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
    /**
     * Создает результат операции "create campaign monster" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse createCampaignMonster(UUID campaignId, MonsterRequest request, String username) {
        return createCampaignMonster(campaignId, request, username, Localization.DEFAULT_LANG);
    }

    /**
     * Создает результат операции "create campaign monster" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse createCampaignMonster(UUID campaignId, MonsterRequest request, String username, String lang) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Monster monster = new Monster();
        applyRequest(monster, request, user, null, campaign);
        Monster saved = monsterRepository.save(monster);
        log.info("Campaign monster created: campaignId={}, id={}, by={}", campaignId, saved.getId(), username);
        if (Boolean.TRUE.equals(saved.getIsVisibleToPlayers())) {
            emitVisibilityEvent(saved, true, user);
        }
        return toResponse(saved, lang);
    }

    /**
     * Выполняет операции "clone monster into campaign" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param sourceId идентификатор source, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse cloneMonsterIntoCampaign(UUID campaignId, UUID sourceId, String username) {
        return cloneMonsterIntoCampaign(campaignId, sourceId, username, Localization.DEFAULT_LANG);
    }

    /**
     * Выполняет операции "clone monster into campaign" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param sourceId идентификатор source, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse cloneMonsterIntoCampaign(UUID campaignId, UUID sourceId, String username, String lang) {
        User user = getUser(username);
        Campaign campaign = campaignService.findCampaign(campaignId);
        campaignService.enforceGmOrAdmin(campaign, user);
        Monster source = findMonster(sourceId);
        enforceCanUseAsCampaignSource(source, campaign, user);
        Monster copy = cloneMonster(source, user);
        copy.setCampaign(campaign);
        copy.setHomebrew(null);
        copy.setIsVisibleToPlayers(false);
        Monster saved = monsterRepository.save(copy);
        log.info("Monster cloned into campaign: campaignId={}, sourceId={}, newId={}, by={}",
                campaignId, sourceId, saved.getId(), username);
        return toResponse(saved, lang);
    }

    /**
     * Обновляет результат операции "update campaign monster" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse updateCampaignMonster(UUID id, MonsterRequest request, String username) {
        return updateCampaignMonster(id, request, username, Localization.DEFAULT_LANG);
    }

    /**
     * Обновляет результат операции "update campaign monster" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse updateCampaignMonster(UUID id, MonsterRequest request, String username, String lang) {
        User user = getUser(username);
        Monster monster = findMonster(id);
        requireScope(monster, "CAMPAIGN");
        campaignService.enforceGmOrAdmin(monster.getCampaign(), user);
        boolean wasVisible = Boolean.TRUE.equals(monster.getIsVisibleToPlayers());
        applyRequest(monster, request, user, null, monster.getCampaign());
        Monster saved = monsterRepository.save(monster);
        boolean nowVisible = Boolean.TRUE.equals(saved.getIsVisibleToPlayers());
        if (nowVisible != wasVisible) {
            emitVisibilityEvent(saved, nowVisible, user);
        }
        return toResponse(saved, lang);
    }

    /**
     * Преобразует данные операции "toggle campaign monster visibility" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse toggleCampaignMonsterVisibility(UUID id, String username) {
        return toggleCampaignMonsterVisibility(id, username, Localization.DEFAULT_LANG);
    }

    /**
     * Преобразует данные операции "toggle campaign monster visibility" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public MonsterResponse toggleCampaignMonsterVisibility(UUID id, String username, String lang) {
        User user = getUser(username);
        Monster monster = findMonster(id);
        requireScope(monster, "CAMPAIGN");
        campaignService.enforceGmOrAdmin(monster.getCampaign(), user);
        monster.setIsVisibleToPlayers(!Boolean.TRUE.equals(monster.getIsVisibleToPlayers()));
        monster.setUpdatedBy(user);
        Monster saved = monsterRepository.save(monster);
        log.info("Campaign monster visibility toggled: id={}, visible={}, by={}",
                id, saved.getIsVisibleToPlayers(), username);
        emitVisibilityEvent(saved, Boolean.TRUE.equals(saved.getIsVisibleToPlayers()), user);
        return toResponse(saved, lang);
    }

    /**
     * Удаляет результат операции "delete campaign monster" в рамках бизнес-логики домена.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void deleteCampaignMonster(UUID id, String username) {
        User user = getUser(username);
        Monster monster = findMonster(id);
        requireScope(monster, "CAMPAIGN");
        campaignService.enforceGmOrAdmin(monster.getCampaign(), user);
        monsterRepository.delete(monster);
        log.info("Campaign monster deleted: id={}, by={}", id, username);
    }

    // =============================== Apply / clone ===============================

    private void applyRequest(Monster monster, MonsterRequest request, User actor,
                              HomebrewPackage pkg, Campaign campaign) {
        monster.setNameRusloc(request.getNameRusloc());
        monster.setNameEngloc(request.getNameEngloc());
        monster.setSlug(resolveSlug(monster, request));
        monster.setAlignment(request.getAlignmentId() == null ? null
                : alignmentRepository.findById(request.getAlignmentId())
                .orElseThrow(() -> new BadRequestException("Alignment not found: " + request.getAlignmentId())));
        if (request.getSizeId() == null) {
            throw new BadRequestException("sizeId is required");
        }
        monster.setSize(resolveOne(request.getSizeId(), bestiarySizeRepository::findById, "size"));
        monster.setSizeSecondary(resolveOptional(request.getSizeSecondaryId(), bestiarySizeRepository::findById, "sizeSecondary"));
        monster.setIsSwarm(request.getIsSwarm() != null ? request.getIsSwarm() : false);
        monster.setSwarmSize(resolveOptional(request.getSwarmSizeId(), bestiarySizeRepository::findById, "swarmSize"));
        monster.setArmorClass(request.getArmorClass());
        monster.setArmorClassText(request.getArmorClassText());
        monster.setInitiativeBonus(request.getInitiativeBonus());
        monster.setInitiativeScore(request.getInitiativeScore());
        monster.setHpAverage(request.getHpAverage());
        monster.setHpDiceCount(request.getHpDiceCount());
        monster.setHpDiceSides(request.getHpDiceSides());
        monster.setHpDiceModifier(request.getHpDiceModifier());
        monster.setHpFormula(request.getHpFormula());
        monster.setStrScore(request.getStrScore());
        monster.setDexScore(request.getDexScore());
        monster.setConScore(request.getConScore());
        monster.setIntScore(request.getIntScore());
        monster.setWisScore(request.getWisScore());
        monster.setChaScore(request.getChaScore());
        monster.setPassivePerception(request.getPassivePerception());
        monster.setTelepathyFt(request.getTelepathyFt());
        monster.setCrRating(request.getCrRating());
        monster.setCrValue(request.getCrValue());
        monster.setXpBase(request.getXpBase());
        monster.setXpLair(request.getXpLair());
        monster.setProficiencyBonus(request.getProficiencyBonus());
        monster.setLegendaryUsesBase(request.getLegendaryUsesBase());
        monster.setLegendaryUsesLair(request.getLegendaryUsesLair());
        monster.setLegendaryText(request.getLegendaryText());
        monster.setLoreText(request.getLoreText());

        monster.setHomebrew(pkg);
        monster.setCampaign(campaign);
        if (request.getIsActive() != null) monster.setIsActive(request.getIsActive());
        if (request.getIsVisibleToPlayers() != null) monster.setIsVisibleToPlayers(request.getIsVisibleToPlayers());
        if (monster.getCreatedBy() == null) monster.setCreatedBy(actor);
        monster.setUpdatedBy(actor);

        monster.setCreatureTypes(resolveSet(request.getCreatureTypeIds(), creatureTypeRepository::findById, "creatureType"));
        monster.setLanguages(resolveSet(request.getLanguageIds(), languageRepository::findById, "language"));
        monster.setConditionImmunities(resolveSet(request.getConditionImmunityIds(), bestiaryConditionRepository::findById, "condition"));
        monster.setHabitats(resolveSet(request.getHabitatIds(), habitatRepository::findById, "habitat"));
        monster.setTreasureTags(resolveSet(request.getTreasureTagIds(), treasureTagRepository::findById, "treasureTag"));
        monster.setSources(resolveSet(request.getSourceIds(), sourceRepository::findById, "source"));

        // On update, clear the child collections and flush so the orphan DELETEs are
        // issued before the rebuild re-INSERTs below. Otherwise Hibernate orders all
        // inserts ahead of deletes in a single flush and an unchanged row (same
        // monster_id + damage_type_id, etc.) trips a unique constraint such as
        // uq_monster_damage_immunities.
        if (monster.getId() != null) {
            monster.getSpeeds().clear();
            monster.getSenses().clear();
            monster.getSavingThrows().clear();
            monster.getSkillProficiencies().clear();
            monster.getDamageResistances().clear();
            monster.getDamageImmunities().clear();
            monster.getDamageVulnerabilities().clear();
            monster.getGear().clear();
            monster.getFeatures().clear();
            monsterRepository.flush();
        }

        rebuildSpeeds(monster, request);
        rebuildSenses(monster, request);
        rebuildSavingThrows(monster, request);
        rebuildSkillProficiencies(monster, request);
        rebuildDamages(monster, request);
        rebuildGear(monster, request);
        rebuildFeatures(monster, request);
    }

    private void rebuildSpeeds(Monster monster, MonsterRequest request) {
        monster.getSpeeds().clear();
        if (request.getSpeeds() == null) return;
        for (MonsterRequest.SpeedEntry e : request.getSpeeds()) {
            MovementType mt = movementTypeRepository.findById(e.getMovementTypeId())
                    .orElseThrow(() -> new BadRequestException("MovementType not found: " + e.getMovementTypeId()));
            monster.getSpeeds().add(MonsterSpeed.builder()
                    .monster(monster).movementType(mt).ft(e.getFt())
                    .hover(e.getHover() != null ? e.getHover() : false).build());
        }
    }

    private void rebuildSenses(Monster monster, MonsterRequest request) {
        monster.getSenses().clear();
        if (request.getSenses() == null) return;
        for (MonsterRequest.SenseEntry e : request.getSenses()) {
            SenseType st = senseTypeRepository.findById(e.getSenseTypeId())
                    .orElseThrow(() -> new BadRequestException("SenseType not found: " + e.getSenseTypeId()));
            monster.getSenses().add(MonsterSense.builder()
                    .monster(monster).senseType(st).ft(e.getFt()).build());
        }
    }

    private void rebuildSavingThrows(Monster monster, MonsterRequest request) {
        monster.getSavingThrows().clear();
        if (request.getSavingThrows() == null) return;
        for (MonsterRequest.SavingThrowEntry e : request.getSavingThrows()) {
            monster.getSavingThrows().add(MonsterSavingThrow.builder()
                    .monster(monster)
                    .ability(resolveOne(e.getAbilityId(), bestiaryAbilityRepository::findById, "savingThrow.ability"))
                    .bonus(e.getBonus()).build());
        }
    }

    private void rebuildSkillProficiencies(Monster monster, MonsterRequest request) {
        monster.getSkillProficiencies().clear();
        if (request.getSkillProficiencies() == null) return;
        for (MonsterRequest.SkillProficiencyEntry e : request.getSkillProficiencies()) {
            ProficiencySkill skill = proficiencySkillRepository.findById(e.getProficiencySkillId())
                    .orElseThrow(() -> new BadRequestException("ProficiencySkill not found: " + e.getProficiencySkillId()));
            monster.getSkillProficiencies().add(MonsterSkillProficiency.builder()
                    .monster(monster).proficiencySkill(skill).bonus(e.getBonus()).build());
        }
    }

    private void rebuildDamages(Monster monster, MonsterRequest request) {
        monster.getDamageResistances().clear();
        if (request.getDamageResistances() != null) {
            for (MonsterRequest.DamageEntry e : request.getDamageResistances()) {
                monster.getDamageResistances().add(MonsterDamageResistance.builder()
                        .monster(monster).damageType(resolveDamageType(e.getDamageTypeId())).note(e.getNote()).build());
            }
        }
        monster.getDamageImmunities().clear();
        if (request.getDamageImmunities() != null) {
            for (MonsterRequest.DamageEntry e : request.getDamageImmunities()) {
                monster.getDamageImmunities().add(MonsterDamageImmunity.builder()
                        .monster(monster).damageType(resolveDamageType(e.getDamageTypeId())).note(e.getNote()).build());
            }
        }
        monster.getDamageVulnerabilities().clear();
        if (request.getDamageVulnerabilities() != null) {
            for (MonsterRequest.DamageEntry e : request.getDamageVulnerabilities()) {
                monster.getDamageVulnerabilities().add(MonsterDamageVulnerability.builder()
                        .monster(monster).damageType(resolveDamageType(e.getDamageTypeId())).note(e.getNote()).build());
            }
        }
    }

    private void rebuildGear(Monster monster, MonsterRequest request) {
        monster.getGear().clear();
        if (request.getGear() == null) return;
        for (MonsterRequest.GearEntry e : request.getGear()) {
            MonsterGearItem item = monsterGearItemRepository.findById(e.getItemId())
                    .orElseThrow(() -> new BadRequestException("Gear item not found: " + e.getItemId()));
            monster.getGear().add(MonsterGear.builder()
                    .monster(monster).item(item)
                    .qty(e.getQty() != null ? e.getQty() : (short) 1).build());
        }
    }

    private void rebuildFeatures(Monster monster, MonsterRequest request) {
        monster.getFeatures().clear();
        if (request.getFeatures() == null) return;
        for (MonsterRequest.FeatureEntry fe : request.getFeatures()) {
            MonsterFeature f = MonsterFeature.builder()
                    .monster(monster)
                    .section(fe.getSection())
                    .sortOrder(fe.getSortOrder())
                    .nameRusloc(fe.getNameRusloc())
                    .nameEngloc(fe.getNameEngloc())
                    .kind(fe.getKind())
                    .rechargeMin(fe.getRechargeMin())
                    .rechargeMax(fe.getRechargeMax())
                    .descriptionRusloc(fe.getDescriptionRusloc())
                    .descriptionEngloc(fe.getDescriptionEngloc())
                    .attackType(fe.getAttackType())
                    .attackBonus(fe.getAttackBonus())
                    .reachFt(fe.getReachFt())
                    .rangeFt(fe.getRangeFt())
                    .rangeLongFt(fe.getRangeLongFt())
                    .saveAbility(resolveOptional(fe.getSaveAbilityId(), bestiaryAbilityRepository::findById, "feature.saveAbility"))
                    .saveDc(fe.getSaveDc())
                    .build();
            if (fe.getDamages() != null) {
                for (MonsterRequest.FeatureDamageEntry de : fe.getDamages()) {
                    f.getDamages().add(FeatureDamage.builder()
                            .feature(f)
                            .sortOrder(de.getSortOrder())
                            .average(de.getAverage())
                            .dice(de.getDice())
                            .damageType(resolveDamageType(de.getDamageTypeId()))
                            .note(de.getNote())
                            .build());
                }
            }
            monster.getFeatures().add(f);
        }
    }

    private Monster cloneMonster(Monster src, User actor) {
        Monster copy = new Monster();
        copy.setNameRusloc(src.getNameRusloc());
        copy.setNameEngloc(src.getNameEngloc());
        copy.setSlug(nextCopySlug(src.getSlug()));
        copy.setAlignment(src.getAlignment());
        copy.setSize(src.getSize());
        copy.setSizeSecondary(src.getSizeSecondary());
        copy.setIsSwarm(src.getIsSwarm());
        copy.setSwarmSize(src.getSwarmSize());
        copy.setArmorClass(src.getArmorClass());
        copy.setArmorClassText(src.getArmorClassText());
        copy.setInitiativeBonus(src.getInitiativeBonus());
        copy.setInitiativeScore(src.getInitiativeScore());
        copy.setHpAverage(src.getHpAverage());
        copy.setHpDiceCount(src.getHpDiceCount());
        copy.setHpDiceSides(src.getHpDiceSides());
        copy.setHpDiceModifier(src.getHpDiceModifier());
        copy.setHpFormula(src.getHpFormula());
        copy.setStrScore(src.getStrScore());
        copy.setDexScore(src.getDexScore());
        copy.setConScore(src.getConScore());
        copy.setIntScore(src.getIntScore());
        copy.setWisScore(src.getWisScore());
        copy.setChaScore(src.getChaScore());
        copy.setPassivePerception(src.getPassivePerception());
        copy.setTelepathyFt(src.getTelepathyFt());
        copy.setCrRating(src.getCrRating());
        copy.setCrValue(src.getCrValue());
        copy.setXpBase(src.getXpBase());
        copy.setXpLair(src.getXpLair());
        copy.setProficiencyBonus(src.getProficiencyBonus());
        copy.setLegendaryUsesBase(src.getLegendaryUsesBase());
        copy.setLegendaryUsesLair(src.getLegendaryUsesLair());
        copy.setLegendaryText(src.getLegendaryText());
        copy.setLoreText(src.getLoreText());
        copy.setIsActive(true);
        copy.setIsVisibleToPlayers(false);
        copy.setSourceMonster(src);
        copy.setCreatedBy(actor);
        copy.setUpdatedBy(actor);

        copy.setCreatureTypes(new HashSet<>(src.getCreatureTypes()));
        copy.setLanguages(new HashSet<>(src.getLanguages()));
        copy.setConditionImmunities(new HashSet<>(src.getConditionImmunities()));
        copy.setHabitats(new HashSet<>(src.getHabitats()));
        copy.setTreasureTags(new HashSet<>(src.getTreasureTags()));
        copy.setSources(new HashSet<>(src.getSources()));

        for (MonsterSpeed s : src.getSpeeds()) {
            copy.getSpeeds().add(MonsterSpeed.builder()
                    .monster(copy).movementType(s.getMovementType()).ft(s.getFt()).hover(s.getHover()).build());
        }
        for (MonsterSense s : src.getSenses()) {
            copy.getSenses().add(MonsterSense.builder()
                    .monster(copy).senseType(s.getSenseType()).ft(s.getFt()).build());
        }
        for (MonsterSavingThrow s : src.getSavingThrows()) {
            copy.getSavingThrows().add(MonsterSavingThrow.builder()
                    .monster(copy).ability(s.getAbility()).bonus(s.getBonus()).build());
        }
        for (MonsterSkillProficiency s : src.getSkillProficiencies()) {
            copy.getSkillProficiencies().add(MonsterSkillProficiency.builder()
                    .monster(copy).proficiencySkill(s.getProficiencySkill()).bonus(s.getBonus()).build());
        }
        for (MonsterDamageResistance s : src.getDamageResistances()) {
            copy.getDamageResistances().add(MonsterDamageResistance.builder()
                    .monster(copy).damageType(s.getDamageType()).note(s.getNote()).build());
        }
        for (MonsterDamageImmunity s : src.getDamageImmunities()) {
            copy.getDamageImmunities().add(MonsterDamageImmunity.builder()
                    .monster(copy).damageType(s.getDamageType()).note(s.getNote()).build());
        }
        for (MonsterDamageVulnerability s : src.getDamageVulnerabilities()) {
            copy.getDamageVulnerabilities().add(MonsterDamageVulnerability.builder()
                    .monster(copy).damageType(s.getDamageType()).note(s.getNote()).build());
        }
        for (MonsterGear s : src.getGear()) {
            copy.getGear().add(MonsterGear.builder()
                    .monster(copy).item(s.getItem()).qty(s.getQty()).build());
        }
        for (MonsterFeature s : src.getFeatures()) {
            MonsterFeature f = MonsterFeature.builder()
                    .monster(copy)
                    .section(s.getSection()).sortOrder(s.getSortOrder())
                    .nameRusloc(s.getNameRusloc()).nameEngloc(s.getNameEngloc())
                    .kind(s.getKind()).rechargeMin(s.getRechargeMin()).rechargeMax(s.getRechargeMax())
                    .descriptionRusloc(s.getDescriptionRusloc()).descriptionEngloc(s.getDescriptionEngloc())
                    .attackType(s.getAttackType()).attackBonus(s.getAttackBonus())
                    .reachFt(s.getReachFt()).rangeFt(s.getRangeFt()).rangeLongFt(s.getRangeLongFt())
                    .saveAbility(s.getSaveAbility()).saveDc(s.getSaveDc())
                    .build();
            for (FeatureDamage d : s.getDamages()) {
                f.getDamages().add(FeatureDamage.builder()
                        .feature(f).sortOrder(d.getSortOrder()).average(d.getAverage())
                        .dice(d.getDice()).damageType(d.getDamageType()).note(d.getNote()).build());
            }
            copy.getFeatures().add(f);
        }
        return copy;
    }

    // ================================ Responses ================================

    private MonsterSummaryResponse toSummary(Monster m, String lang) {
        return MonsterSummaryResponse.builder()
                .id(m.getId())
                .slug(m.getSlug())
                .name(localizedName(lang, m.getNameRusloc(), m.getNameEngloc()))
                .nameRusloc(m.getNameRusloc())
                .nameEngloc(m.getNameEngloc())
                .size(m.getSize() != null ? ref(m.getSize(), lang) : null)
                .crRating(m.getCrRating())
                .crValue(m.getCrValue())
                .scope(scopeOf(m))
                .homebrewId(m.getHomebrew() != null ? m.getHomebrew().getId() : null)
                .campaignId(m.getCampaign() != null ? m.getCampaign().getId() : null)
                .isVisibleToPlayers(m.getIsVisibleToPlayers())
                .isActive(m.getIsActive())
                .build();
    }

    private MonsterResponse toResponse(Monster m) {
        return toResponse(m, Localization.DEFAULT_LANG);
    }

    private MonsterResponse toResponse(Monster m, String lang) {
        return MonsterResponse.builder()
                .id(m.getId())
                .sourceExternalId(m.getSourceExternalId())
                .slug(m.getSlug())
                .name(localizedName(lang, m.getNameRusloc(), m.getNameEngloc()))
                .nameRusloc(m.getNameRusloc())
                .nameEngloc(m.getNameEngloc())
                .alignment(m.getAlignment() != null ? ref(m.getAlignment(), lang) : null)
                .size(m.getSize() != null ? ref(m.getSize(), lang) : null)
                .sizeSecondary(m.getSizeSecondary() != null ? ref(m.getSizeSecondary(), lang) : null)
                .isSwarm(m.getIsSwarm())
                .swarmSize(m.getSwarmSize() != null ? ref(m.getSwarmSize(), lang) : null)
                .armorClass(m.getArmorClass())
                .armorClassText(m.getArmorClassText())
                .initiativeBonus(m.getInitiativeBonus())
                .initiativeScore(m.getInitiativeScore())
                .hpAverage(m.getHpAverage())
                .hpDiceCount(m.getHpDiceCount())
                .hpDiceSides(m.getHpDiceSides())
                .hpDiceModifier(m.getHpDiceModifier())
                .hpFormula(m.getHpFormula())
                .strScore(m.getStrScore())
                .dexScore(m.getDexScore())
                .conScore(m.getConScore())
                .intScore(m.getIntScore())
                .wisScore(m.getWisScore())
                .chaScore(m.getChaScore())
                .passivePerception(m.getPassivePerception())
                .telepathyFt(m.getTelepathyFt())
                .crRating(m.getCrRating())
                .crValue(m.getCrValue())
                .xpBase(m.getXpBase())
                .xpLair(m.getXpLair())
                .proficiencyBonus(m.getProficiencyBonus())
                .legendaryUsesBase(m.getLegendaryUsesBase())
                .legendaryUsesLair(m.getLegendaryUsesLair())
                .legendaryText(m.getLegendaryText())
                .loreText(m.getLoreText())
                .scope(scopeOf(m))
                .homebrewId(m.getHomebrew() != null ? m.getHomebrew().getId() : null)
                .campaignId(m.getCampaign() != null ? m.getCampaign().getId() : null)
                .sourceMonsterId(m.getSourceMonster() != null ? m.getSourceMonster().getId() : null)
                .isVisibleToPlayers(m.getIsVisibleToPlayers())
                .isActive(m.getIsActive())
                .createdBy(m.getCreatedBy() != null ? m.getCreatedBy().getId() : null)
                .createdByUsername(m.getCreatedBy() != null ? m.getCreatedBy().getUsername() : null)
                .updatedBy(m.getUpdatedBy() != null ? m.getUpdatedBy().getId() : null)
                .updatedByUsername(m.getUpdatedBy() != null ? m.getUpdatedBy().getUsername() : null)
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .creatureTypes(refList(m.getCreatureTypes(), lang))
                .languages(refList(m.getLanguages(), lang))
                .conditionImmunities(refList(m.getConditionImmunities(), lang))
                .habitats(refList(m.getHabitats(), lang))
                .treasureTags(refList(m.getTreasureTags(), lang))
                .sources(refList(m.getSources(), lang))
                .speeds(m.getSpeeds().stream().map(s -> MonsterResponse.SpeedView.builder()
                        .id(s.getId()).movementType(ref(s.getMovementType(), lang)).ft(s.getFt()).hover(s.getHover()).build()).toList())
                .senses(m.getSenses().stream().map(s -> MonsterResponse.SenseView.builder()
                        .id(s.getId()).senseType(ref(s.getSenseType(), lang)).ft(s.getFt()).build()).toList())
                .savingThrows(m.getSavingThrows().stream().map(s -> MonsterResponse.SavingThrowView.builder()
                        .id(s.getId()).ability(s.getAbility() != null ? ref(s.getAbility(), lang) : null).bonus(s.getBonus()).build()).toList())
                .skillProficiencies(m.getSkillProficiencies().stream().map(s -> MonsterResponse.SkillProficiencyView.builder()
                        .id(s.getId()).proficiencySkillId(s.getProficiencySkill().getId())
                        .skillName(Localization.pick(lang, s.getProficiencySkill().getNameRusloc(),
                                s.getProficiencySkill().getNameEngloc(), s.getProficiencySkill().getName()))
                        .bonus(s.getBonus()).build()).toList())
                .damageResistances(m.getDamageResistances().stream().map(d -> MonsterResponse.DamageView.builder()
                        .id(d.getId()).damageType(d.getDamageType() != null ? ref(d.getDamageType(), lang) : null).note(d.getNote()).build()).toList())
                .damageImmunities(m.getDamageImmunities().stream().map(d -> MonsterResponse.DamageView.builder()
                        .id(d.getId()).damageType(d.getDamageType() != null ? ref(d.getDamageType(), lang) : null).note(d.getNote()).build()).toList())
                .damageVulnerabilities(m.getDamageVulnerabilities().stream().map(d -> MonsterResponse.DamageView.builder()
                        .id(d.getId()).damageType(d.getDamageType() != null ? ref(d.getDamageType(), lang) : null).note(d.getNote()).build()).toList())
                .gear(m.getGear().stream().map(g -> MonsterResponse.GearView.builder()
                        .id(g.getId()).item(ref(g.getItem(), lang)).qty(g.getQty()).build()).toList())
                .features(m.getFeatures().stream().map(f -> featureView(f, lang)).toList())
                .build();
    }

    private MonsterResponse.FeatureView featureView(MonsterFeature f, String lang) {
        return MonsterResponse.FeatureView.builder()
                .id(f.getId())
                .section(f.getSection())
                .sortOrder(f.getSortOrder())
                .name(localizedName(lang, f.getNameRusloc(), f.getNameEngloc()))
                .nameRusloc(f.getNameRusloc())
                .nameEngloc(f.getNameEngloc())
                .kind(f.getKind())
                .rechargeMin(f.getRechargeMin())
                .rechargeMax(f.getRechargeMax())
                .description(localizedText(lang, f.getDescriptionRusloc(), f.getDescriptionEngloc()))
                .descriptionRusloc(f.getDescriptionRusloc())
                .descriptionEngloc(f.getDescriptionEngloc())
                .attackType(f.getAttackType())
                .attackBonus(f.getAttackBonus())
                .reachFt(f.getReachFt())
                .rangeFt(f.getRangeFt())
                .rangeLongFt(f.getRangeLongFt())
                .saveAbility(f.getSaveAbility() != null ? ref(f.getSaveAbility(), lang) : null)
                .saveDc(f.getSaveDc())
                .damages(f.getDamages().stream().map(d -> MonsterResponse.FeatureDamageView.builder()
                        .id(d.getId()).sortOrder(d.getSortOrder()).average(d.getAverage())
                        .dice(d.getDice()).damageType(d.getDamageType() != null ? ref(d.getDamageType(), lang) : null)
                        .note(d.getNote()).build()).toList())
                .build();
    }

    private MonsterResponse.DictionaryRef ref(DictionaryEntry e, String lang) {
        return MonsterResponse.DictionaryRef.builder()
                .id(e.getId()).code(e.getCode())
                .name(localizedName(lang, e.getNameRusloc(), e.getNameEngloc()))
                .nameRusloc(e.getNameRusloc()).nameEngloc(e.getNameEngloc())
                .homebrewId(e.getHomebrew() != null ? e.getHomebrew().getId() : null)
                .build();
    }

    private List<MonsterResponse.DictionaryRef> refList(Set<? extends DictionaryEntry> set, String lang) {
        return set.stream().map(e -> ref(e, lang))
                .sorted((a, b) -> a.getCode().compareToIgnoreCase(b.getCode()))
                .collect(Collectors.toList());
    }

    private String localizedName(String lang, String rusloc, String engloc) {
        return Localization.pick(lang, rusloc, engloc, rusloc);
    }

    private String localizedText(String lang, String rusloc, String engloc) {
        return Localization.pick(lang, rusloc, engloc, rusloc);
    }

    // ================================ Helpers ================================

    private void enforceCanRead(Monster monster, User user) {
        if (monster.getCampaign() != null) {
            campaignService.enforceMembershipOrAdmin(monster.getCampaign(), user);
            boolean gm = isGmOrAdmin(monster.getCampaign().getId(), user);
            if (!gm && !Boolean.TRUE.equals(monster.getIsVisibleToPlayers())) {
                throw new ResourceNotFoundException("Monster not found");
            }
            return;
        }
        // SEC-1 / P0-1: homebrew-монстр читаем только через guard пакета (чужой — только если PUBLISHED).
        if (monster.getHomebrew() != null) {
            homebrewAccessService.enforceReadable(monster.getHomebrew().getId(), user);
            return;
        }
        // SYSTEM monsters are readable by any authenticated user.
    }

    private void enforceCanUseAsCampaignSource(Monster source, Campaign campaign, User user) {
        String scope = scopeOf(source);
        if ("SYSTEM".equals(scope)) return;
        if ("CAMPAIGN".equals(scope)) {
            if (source.getCampaign().getId().equals(campaign.getId())) return;
            throw new BadRequestException("Monster belongs to a different campaign");
        }
        // HOMEBREW: allowed only if the package is attached to this campaign
        Set<UUID> packageIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaign.getId());
        if (!packageIds.contains(source.getHomebrew().getId())) {
            throw new BadRequestException("Source monster's homebrew is not available in this campaign");
        }
    }

    private boolean isGmOrAdmin(UUID campaignId, User user) {
        return user.getRole() == Role.ADMIN || campaignService.isGmInCampaign(campaignId, user.getId());
    }

    private void emitVisibilityEvent(Monster monster, boolean nowVisible, User actor) {
        if (monster.getCampaign() == null) return;
        webSocketEventService.sendCampaignEvent(
                nowVisible ? WebSocketEventType.MONSTER_REVEALED : WebSocketEventType.MONSTER_HIDDEN,
                monster.getCampaign().getId(),
                Map.of("monsterId", monster.getId(), "monsterName", monster.getNameRusloc()),
                actor.getId());
    }

    private String scopeOf(Monster m) {
        if (m.getCampaign() != null) return "CAMPAIGN";
        if (m.getHomebrew() != null) return "HOMEBREW";
        return "SYSTEM";
    }

    private void requireScope(Monster monster, String scope) {
        if (!scope.equals(scopeOf(monster))) {
            throw new BadRequestException("Monster is not a " + scope.toLowerCase() + " monster");
        }
    }

    private void requireSameHomebrew(Monster monster, HomebrewPackage pkg) {
        if (monster.getHomebrew() == null || !monster.getHomebrew().getId().equals(pkg.getId())) {
            throw new AccessDeniedException("Monster does not belong to this homebrew package");
        }
    }

    private void attachToPackage(HomebrewPackage pkg, UUID monsterId) {
        if (!contentItemRepository.existsByHomebrewPackageIdAndContentTypeAndContentId(pkg.getId(), ContentType.MONSTER, monsterId)) {
            contentItemRepository.save(HomebrewContentItem.builder()
                    .homebrewPackage(pkg)
                    .contentType(ContentType.MONSTER)
                    .contentId(monsterId)
                    .build());
        }
    }

    private HomebrewPackage getEditablePackage(UUID packageId, User gm) {
        HomebrewPackage pkg = homebrewPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Homebrew package not found"));
        if (gm.getRole() != Role.ADMIN && !pkg.getAuthor().getId().equals(gm.getId())) {
            throw new AccessDeniedException("Cannot manage monsters in another user's homebrew package");
        }
        if (pkg.isDeleted() || !pkg.getStatus().isEditable()) {
            throw new BadRequestException("Homebrew monster content can be changed only in a DRAFT or PUBLISHED package");
        }
        return pkg;
    }

    private <T extends DictionaryEntry> Set<T> resolveSet(Set<UUID> ids,
                                                          Function<UUID, java.util.Optional<T>> finder,
                                                          String label) {
        Set<T> result = new HashSet<>();
        if (ids == null) return result;
        for (UUID id : ids) {
            result.add(finder.apply(id)
                    .orElseThrow(() -> new BadRequestException(label + " not found: " + id)));
        }
        return result;
    }

    private <T extends DictionaryEntry> T resolveOne(UUID id,
                                                     Function<UUID, java.util.Optional<T>> finder,
                                                     String label) {
        if (id == null) {
            throw new BadRequestException(label + " is required");
        }
        return finder.apply(id)
                .orElseThrow(() -> new BadRequestException(label + " not found: " + id));
    }

    private <T extends DictionaryEntry> T resolveOptional(UUID id,
                                                          Function<UUID, java.util.Optional<T>> finder,
                                                          String label) {
        if (id == null) return null;
        return finder.apply(id)
                .orElseThrow(() -> new BadRequestException(label + " not found: " + id));
    }

    private DamageType resolveDamageType(UUID id) {
        return resolveOptional(id, damageTypeRepository::findById, "damageType");
    }

    private String resolveSlug(Monster monster, MonsterRequest request) {
        if (request.getSlug() != null && !request.getSlug().isBlank()) {
            String requested = request.getSlug().trim();
            if (!requested.equals(monster.getSlug()) && monsterRepository.existsBySlug(requested)) {
                throw new DuplicateResourceException("Monster with slug '" + requested + "' already exists");
            }
            return requested;
        }
        if (monster.getSlug() != null) {
            return monster.getSlug();
        }
        String base = slugify(request.getNameEngloc() != null ? request.getNameEngloc() : request.getNameRusloc());
        return uniqueSlug(base);
    }

    private String nextCopySlug(String sourceSlug) {
        return uniqueSlug(slugify(sourceSlug) + "-copy");
    }

    private String uniqueSlug(String base) {
        String candidate = base;
        int suffix = 2;
        while (monsterRepository.existsBySlug(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }

    private String slugify(String value) {
        if (value == null) return "monster-" + UUID.randomUUID();
        String slug = value.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        return slug.isBlank() ? "monster-" + UUID.randomUUID() : slug;
    }

    private Monster findMonster(UUID id) {
        return monsterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Monster not found"));
    }

    private User requireGameMaster(String username) {
        User user = getUser(username);
        if (user.getRole() != Role.GAME_MASTER && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only game masters can manage homebrew monsters");
        }
        return user;
    }

    private User requireRole(String username, Role role) {
        User user = getUser(username);
        if (user.getRole() != role) {
            throw new AccessDeniedException("Required role: " + role);
        }
        return user;
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
