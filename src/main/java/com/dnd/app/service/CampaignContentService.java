package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.enums.ContentType;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.ActivateHomebrewRequest;
import com.dnd.app.dto.request.UpdatePinnedVersionRequest;
import com.dnd.app.dto.response.CampaignHomebrewResponse;
import com.dnd.app.dto.response.CampaignAvailableContentResponse;
import com.dnd.app.dto.response.CampaignAvailableContentResponse.AvailableContentItem;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Класс CampaignContentService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CampaignContentService {

    private final CampaignHomebrewRepository campaignHomebrewRepository;
    private final HomebrewPackageRepository homebrewPackageRepository;
    private final HomebrewContentItemRepository contentItemRepository;
    private final ContentCharacterClassRepository classRepository;
    private final SpeciesRepository speciesRepository;
    private final ItemTypeRepository itemTypeRepository;
    private final SkillRepository skillRepository;
    private final FeatRepository featRepository;
    private final UserRepository userRepository;
    private final CampaignService campaignService;
    private final PlayerCharacterRepository playerCharacterRepository;

    /**
     * Выполняет операции "activate homebrew" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public CampaignHomebrewResponse activateHomebrew(UUID campaignId, ActivateHomebrewRequest request, String username) {
        Campaign campaign = campaignService.findCampaign(campaignId);
        User user = getUser(username);
        campaignService.enforceGmOrAdmin(campaign, user);

        HomebrewPackage pkg = homebrewPackageRepository.findById(request.getHomebrewPackageId())
                .orElseThrow(() -> new ResourceNotFoundException("Homebrew-пакет не найден"));

        if (pkg.getStatus() != HomebrewStatus.PUBLISHED || pkg.isDeleted()) {
            throw new ResourceNotFoundException("Homebrew-пакет не найден или не опубликован");
        }

        if (campaignHomebrewRepository.existsByCampaignIdAndPackageId(campaignId, pkg.getId())) {
            throw new DuplicateResourceException("Этот пакет уже активирован для данной кампании");
        }

        CampaignHomebrew activation = CampaignHomebrew.builder()
                .campaignId(campaignId)
                .packageId(pkg.getId())
                .build();
        campaignHomebrewRepository.save(activation);

        log.info("Homebrew activated: package='{}', campaignId={}, by user={}", pkg.getTitle(), campaignId, username);

        return buildResponse(pkg, null);
    }

    /**
     * Выполняет операции "deactivate homebrew" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void deactivateHomebrew(UUID campaignId, UUID packageId, String username, boolean force) {
        Campaign campaign = campaignService.findCampaign(campaignId);
        User user = getUser(username);
        campaignService.enforceGmOrAdmin(campaign, user);

        if (!campaignHomebrewRepository.existsByCampaignIdAndPackageId(campaignId, packageId)) {
            throw new ResourceNotFoundException("Активация пакета не найдена для этой кампании");
        }

        // P1-2: не отвязываем пакет, если его контент ещё используется персонажами кампании —
        // иначе их листы получат осиротевшие ссылки на класс/вид/предысторию. При force=true
        // отвязываем принудительно (такие персонажи потеряют доступ к контенту пакета).
        if (!force) {
            Map<ContentType, Set<UUID>> byType = contentItemRepository.findAllByHomebrewPackageId(packageId).stream()
                    .collect(Collectors.groupingBy(HomebrewContentItem::getContentType,
                            Collectors.mapping(HomebrewContentItem::getContentId, Collectors.toSet())));

            long classDeps = countDeps(byType, ContentType.CHARACTER_CLASS,
                    ids -> playerCharacterRepository.countInCampaignUsingClasses(campaignId, ids));
            long speciesDeps = countDeps(byType, ContentType.SPECIES,
                    ids -> playerCharacterRepository.countByCampaignIdAndRaceIdIn(campaignId, ids));
            long backgroundDeps = countDeps(byType, ContentType.BACKGROUND,
                    ids -> playerCharacterRepository.countByCampaignIdAndBackgroundIdIn(campaignId, ids));

            long total = classDeps + speciesDeps + backgroundDeps;
            if (total > 0) {
                throw new DuplicateResourceException(String.format(
                        "Нельзя отвязать пакет: в кампании есть персонажи, использующие его контент " +
                        "(классы: %d, виды: %d, предыстории: %d). Повторите с force=true, чтобы отвязать " +
                        "принудительно — такие персонажи потеряют доступ к контенту пакета.",
                        classDeps, speciesDeps, backgroundDeps));
            }
        }

        campaignHomebrewRepository.deleteByCampaignIdAndPackageId(campaignId, packageId);
        log.info("Homebrew deactivated: packageId={}, campaignId={}, force={}, by user={}",
                packageId, campaignId, force, username);
    }

    /**
     * Считает зависимости персонажей по типу контента, если у пакета есть контент этого типа.
     * @param byType контент пакета, сгруппированный по типу
     * @param type проверяемый тип контента
     * @param counter функция подсчёта по набору id
     * @return количество зависимых персонажей (0, если контента этого типа нет)
     */
    private long countDeps(Map<ContentType, Set<UUID>> byType, ContentType type, Function<Set<UUID>, Long> counter) {
        Set<UUID> ids = byType.get(type);
        if (ids == null || ids.isEmpty()) {
            return 0L;
        }
        return counter.apply(ids);
    }

    /**
     * Обновляет результат операции "update pinned version" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public CampaignHomebrewResponse updatePinnedVersion(UUID campaignId, UUID packageId,
                                                         UpdatePinnedVersionRequest request, String username) {
        Campaign campaign = campaignService.findCampaign(campaignId);
        User user = getUser(username);
        campaignService.enforceGmOrAdmin(campaign, user);

        CampaignHomebrewId id = new CampaignHomebrewId(campaignId, packageId);
        CampaignHomebrew activation = campaignHomebrewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Homebrew package is not attached to this campaign"));

        HomebrewPackage pkg = homebrewPackageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Homebrew package not found"));

        if (request.getPinnedVersion() != null && request.getPinnedVersion() > pkg.getVersion()) {
            throw new com.dnd.app.exception.BadRequestException(
                    "Pinned version cannot exceed the latest published version (" + pkg.getVersion() + ")");
        }

        activation.setPinnedVersion(request.getPinnedVersion());
        campaignHomebrewRepository.save(activation);

        log.info("Pinned version updated: packageId={}, campaignId={}, pinnedVersion={}, by={}",
                packageId, campaignId, request.getPinnedVersion(), username);
        return buildResponse(pkg, request.getPinnedVersion());
    }

    /**
     * Возвращает список для операции "list active homebrew" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<CampaignHomebrewResponse> listActiveHomebrew(UUID campaignId, String username) {
        Campaign campaign = campaignService.findCampaign(campaignId);
        User user = getUser(username);
        campaignService.enforceMembershipOrAdmin(campaign, user);

        List<CampaignHomebrew> activations = campaignHomebrewRepository.findByCampaignId(campaignId);
        return activations.stream().map(a -> {
            HomebrewPackage pkg = a.getHomebrewPackage();
            return buildResponse(pkg, a.getPinnedVersion());
        }).toList();
    }

    /**
     * Возвращает результат операции "get available content" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public CampaignAvailableContentResponse getAvailableContent(UUID campaignId, String username) {
        Campaign campaign = campaignService.findCampaign(campaignId);
        User user = getUser(username);
        campaignService.enforceMembershipOrAdmin(campaign, user);

        Set<UUID> activePackageIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);

        List<AvailableContentItem> classes = new ArrayList<>();
        classRepository.findAllByHomebrewIsNull().forEach(c ->
                classes.add(AvailableContentItem.builder()
                        .id(c.getId()).name(c.getNameRu()).source("GLOBAL").build()));
        if (!activePackageIds.isEmpty()) {
            classRepository.findAllByHomebrewIdIn(activePackageIds).forEach(c ->
                    classes.add(AvailableContentItem.builder()
                            .id(c.getId()).name(c.getNameRu()).source("HOMEBREW")
                            .homebrewTitle(c.getHomebrew() != null ? c.getHomebrew().getTitle() : null)
                            .build()));
        }

        // "races" bucket now lists new content-model species (D&D 2024); legacy races detached in S5.
        List<AvailableContentItem> races = new ArrayList<>();
        speciesRepository.findAllByHomebrewIsNull().forEach(sp ->
                races.add(AvailableContentItem.builder()
                        .id(sp.getId())
                        .name(sp.getNameRu() != null ? sp.getNameRu() : sp.getNameEn())
                        .source("GLOBAL").build()));
        if (!activePackageIds.isEmpty()) {
            speciesRepository.findAllByHomebrewIdIn(activePackageIds).forEach(sp ->
                    races.add(AvailableContentItem.builder()
                            .id(sp.getId())
                            .name(sp.getNameRu() != null ? sp.getNameRu() : sp.getNameEn())
                            .source("HOMEBREW")
                            .homebrewTitle(sp.getHomebrew() != null ? sp.getHomebrew().getTitle() : null)
                            .build()));
        }

        List<AvailableContentItem> itemTypes = new ArrayList<>();
        itemTypeRepository.findAllByHomebrewIsNull().forEach(it ->
                itemTypes.add(AvailableContentItem.builder()
                        .id(it.getId()).name(it.getName()).source("GLOBAL").build()));
        if (!activePackageIds.isEmpty()) {
            itemTypeRepository.findAllByHomebrewIdIn(activePackageIds).forEach(it ->
                    itemTypes.add(AvailableContentItem.builder()
                            .id(it.getId()).name(it.getName()).source("HOMEBREW")
                            .homebrewTitle(it.getHomebrew() != null ? it.getHomebrew().getTitle() : null)
                            .build()));
        }

        List<AvailableContentItem> skills = new ArrayList<>();
        skillRepository.findAllByHomebrewIsNull().forEach(s ->
                skills.add(AvailableContentItem.builder()
                        .id(s.getId()).name(s.getName()).source("GLOBAL").build()));
        if (!activePackageIds.isEmpty()) {
            skillRepository.findAllByHomebrewIdIn(activePackageIds).forEach(s ->
                    skills.add(AvailableContentItem.builder()
                            .id(s.getId()).name(s.getName()).source("HOMEBREW")
                            .homebrewTitle(s.getHomebrew() != null ? s.getHomebrew().getTitle() : null)
                            .build()));
        }

        List<AvailableContentItem> feats = new ArrayList<>();
        featRepository.findAllByHomebrewIsNull().forEach(f ->
                feats.add(AvailableContentItem.builder()
                        .id(f.getId()).name(f.getNameRu()).source("GLOBAL").build()));
        if (!activePackageIds.isEmpty()) {
            featRepository.findAllByHomebrewIdIn(activePackageIds).forEach(f ->
                    feats.add(AvailableContentItem.builder()
                            .id(f.getId()).name(f.getNameRu()).source("HOMEBREW")
                            .homebrewTitle(f.getHomebrew() != null ? f.getHomebrew().getTitle() : null)
                            .build()));
        }

        return CampaignAvailableContentResponse.builder()
                .classes(classes).races(races).itemTypes(itemTypes).skills(skills).feats(feats)
                .build();
    }

    /**
     * Проверяет условие операции "is class available in campaign" в рамках бизнес-логики домена.
     * @param campaignId идентификатор campaign, используемый для выбора нужного бизнес-объекта
     * @param classId идентификатор class, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    public boolean isClassAvailableInCampaign(UUID campaignId, UUID classId) {
        ContentCharacterClass cc = classRepository.findById(classId).orElse(null);
        if (cc == null) return false;
        if (cc.getHomebrew() == null) return true;
        Set<UUID> activePackageIds = campaignHomebrewRepository.findPackageIdsByCampaignId(campaignId);
        return activePackageIds.contains(cc.getHomebrew().getId());
    }

    private CampaignHomebrewResponse buildResponse(HomebrewPackage pkg, Integer pinnedVersion) {
        List<Object[]> countsByType = contentItemRepository.countByPackageGroupedByType(pkg.getId());
        Map<String, Long> contentSummary = new LinkedHashMap<>();
        for (Object[] row : countsByType) {
            ContentType ct = (ContentType) row[0];
            Long count = (Long) row[1];
            contentSummary.put(ct.name().toLowerCase() + "Count", count);
        }
        return CampaignHomebrewResponse.builder()
                .packageId(pkg.getId())
                .title(pkg.getTitle())
                .pinnedVersion(pinnedVersion)
                .contentSummary(contentSummary)
                .build();
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }
}
