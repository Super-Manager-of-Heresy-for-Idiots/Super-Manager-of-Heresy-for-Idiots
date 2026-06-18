package com.dnd.app.service.homebrew;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.ContentType;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.domain.enums.SkillActivation;
import com.dnd.app.dto.request.*;
import com.dnd.app.dto.response.*;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.exception.UnprocessableEntityException;
import com.dnd.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomebrewAuthoringService {

    private final HomebrewPackageRepository packageRepository;
    private final HomebrewContentItemRepository contentItemRepository;
    private final GmHomebrewLibraryRepository gmLibraryRepository;
    private final UserRepository userRepository;
    private final TagService tagService;
    private final HomebrewContentValidatorRegistry validatorRegistry;
    private final ItemTypeRepository itemTypeRepository;
    private final SkillRepository skillRepository;
    private final FeatRepository featRepository;
    private final BuffDebuffRepository buffDebuffRepository;
    private final StatTypeRepository statTypeRepository;
    private final com.dnd.app.service.ContentDictionaryResolver contentDictionaryResolver;

    @Transactional
    public HomebrewDetailResponse createPackage(CreateHomebrewRequest request, String username) {
        User gm = getGameMaster(username);
        Set<HomebrewTag> tags = tagService.findOrCreateTags(request.getTagNames());

        HomebrewPackage pkg = HomebrewPackage.builder()
                .author(gm)
                .title(request.getTitle())
                .description(request.getDescription())
                .tags(tags)
                .build();
        pkg = packageRepository.save(pkg);
        log.info("Homebrew package created: id={}, title='{}', by={}", pkg.getId(), pkg.getTitle(), username);
        return toDetailResponse(pkg);
    }

    @Transactional(readOnly = true)
    public Page<HomebrewPackageResponse> listMyPackages(String username, String status, Pageable pageable) {
        User gm = getGameMaster(username);

        Page<HomebrewPackage> packages;
        if ("DELETED".equalsIgnoreCase(status)) {
            packages = packageRepository.findAllByAuthorIdAndDeletedAtIsNotNull(gm.getId(), pageable);
        } else if (status != null) {
            HomebrewStatus homebrewStatus = parseStatus(status);
            packages = packageRepository.findAllByAuthorIdAndStatus(gm.getId(), homebrewStatus, pageable);
        } else {
            packages = packageRepository.findAllByAuthorId(gm.getId(), pageable);
        }
        return packages.map(this::toPackageResponse);
    }

    @Transactional(readOnly = true)
    public HomebrewDetailResponse getMyPackage(UUID id, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(id, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));
        return toDetailResponse(pkg);
    }

    @Transactional
    public HomebrewDetailResponse updatePackage(UUID id, UpdateHomebrewRequest request, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(id, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (pkg.getStatus() != HomebrewStatus.DRAFT) {
            throw new BadRequestException("Пакет можно редактировать только в статусе черновика (DRAFT)");
        }

        if (request.getTitle() != null) {
            pkg.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            pkg.setDescription(request.getDescription());
        }
        if (request.getTagNames() != null) {
            Set<HomebrewTag> tags = tagService.findOrCreateTags(request.getTagNames());
            pkg.setTags(tags);
        }

        pkg = packageRepository.save(pkg);
        log.info("Homebrew package updated: id={}, by={}", id, username);
        return toDetailResponse(pkg);
    }

    @Transactional
    public HomebrewDetailResponse addContent(UUID packageId, AddContentRequest request, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(packageId, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (pkg.getStatus() != HomebrewStatus.DRAFT) {
            throw new BadRequestException("Контент можно добавлять только в статусе черновика (DRAFT)");
        }

        String contentTypeStr = request.getContentType();
        if (!validatorRegistry.isKnownType(contentTypeStr)) {
            throw new BadRequestException("Неизвестный тип контента: " + contentTypeStr);
        }

        validatorRegistry.validate(contentTypeStr, request.getContentId());

        UUID ownerId = validatorRegistry.getOwnerId(contentTypeStr, request.getContentId());
        if (ownerId != null && !ownerId.equals(gm.getId())) {
            throw new AccessDeniedException("Этот контент вам не принадлежит");
        }

        ContentType contentType = ContentType.valueOf(contentTypeStr);
        if (contentItemRepository.existsByHomebrewPackageIdAndContentTypeAndContentId(
                packageId, contentType, request.getContentId())) {
            throw new DuplicateResourceException("Этот контент уже есть в пакете");
        }

        HomebrewContentItem item = HomebrewContentItem.builder()
                .homebrewPackage(pkg)
                .contentType(contentType)
                .contentId(request.getContentId())
                .build();
        contentItemRepository.save(item);
        log.info("Content added to package: packageId={}, type={}, contentId={}", packageId, contentTypeStr, request.getContentId());

        return toDetailResponse(packageRepository.findById(packageId).orElseThrow());
    }

    @Transactional
    public HomebrewDetailResponse createPackageItemType(UUID packageId, CreateItemTypeRequest request, String username) {
        User gm = getRequiredGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);

        if (itemTypeRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Тип предмета с таким названием уже существует");
        }
        validateDamageFields(request.getDamageDice(), request.getDamageType(), pkg);
        validateItemTypeSkillFields(request.getSkillId(), request.getSkillActivation());

        ItemType itemType = ItemType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .slot(parseSlot(request.getSlot(), pkg))
                .homebrew(pkg)
                .damageDice(request.getDamageDice())
                .damageBonus(request.getDamageBonus() != null ? request.getDamageBonus() : 0)
                .damageType(parseDamageType(request.getDamageType(), pkg))
                .build();
        if (request.getSkillId() != null) {
            Skill skill = getAllowedPackageSkill(request.getSkillId(), pkg);
            itemType.setSkill(skill);
            itemType.setSkillActivation(parseSkillActivation(request.getSkillActivation()));
        }

        ItemType saved = itemTypeRepository.save(itemType);
        attachContentItem(pkg, ContentType.ITEM_TYPE, saved.getId(), username);
        return toDetailResponse(pkg);
    }

    @Transactional
    public HomebrewDetailResponse createPackageSkill(UUID packageId, CreateSkillRequest request, String username) {
        User gm = getRequiredGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);

        if (skillRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Умение с таким названием уже существует");
        }
        validateDamageFields(request.getDamageDice(), request.getDamageType(), pkg);

        Skill skill = Skill.builder()
                .name(request.getName())
                .description(request.getDescription())
                .skillType(request.getSkillType())
                .homebrew(pkg)
                .damageDice(request.getDamageDice())
                .damageBonus(request.getDamageBonus() != null ? request.getDamageBonus() : 0)
                .damageType(parseDamageType(request.getDamageType(), pkg))
                .build();
        Skill saved = skillRepository.save(skill);
        attachContentItem(pkg, ContentType.SKILL, saved.getId(), username);
        return toDetailResponse(pkg);
    }

    @Transactional
    public HomebrewDetailResponse createPackageFeat(UUID packageId, CreateFeatRequest request, String username) {
        User gm = getRequiredGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);

        if (featRepository.existsByNameRu(request.getName())) {
            throw new DuplicateResourceException("Черта с таким названием уже существует");
        }

        Feat feat = Feat.builder()
                .slug(UUID.randomUUID().toString())
                .nameRu(request.getName())
                .description(request.getDescription())
                .homebrew(pkg)
                .build();
        Feat saved = featRepository.save(feat);
        attachContentItem(pkg, ContentType.FEAT, saved.getId(), username);
        return toDetailResponse(pkg);
    }

    @Transactional
    public HomebrewDetailResponse createPackageBuffDebuff(UUID packageId, CreateBuffDebuffRequest request, String username) {
        User gm = getRequiredGameMaster(username);
        HomebrewPackage pkg = getEditablePackage(packageId, gm);

        BuffDebuff saved = createBuffDebuffInPackage(pkg, request, username);
        log.info("Homebrew buff/debuff created: packageId={}, buffDebuffId={}, author={}",
                packageId, saved.getId(), username);
        return toDetailResponse(packageRepository.findById(packageId).orElseThrow());
    }

    @Transactional
    public void removeContent(UUID packageId, UUID contentItemId, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(packageId, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (pkg.getStatus() != HomebrewStatus.DRAFT) {
            throw new BadRequestException("Контент можно удалять только в статусе черновика (DRAFT)");
        }

        HomebrewContentItem item = contentItemRepository.findById(contentItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Элемент контента не найден"));

        if (!item.getHomebrewPackage().getId().equals(packageId)) {
            throw new ResourceNotFoundException("Элемент контента не найден в этом пакете");
        }

        contentItemRepository.delete(item);
        log.info("Content removed from package: packageId={}, contentItemId={}", packageId, contentItemId);
    }

    @Transactional
    public HomebrewDetailResponse publish(UUID id, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(id, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (pkg.getStatus() != HomebrewStatus.DRAFT) {
            throw new BadRequestException("Пакет можно опубликовать только из статуса черновика (DRAFT)");
        }

        long contentCount = contentItemRepository.countByHomebrewPackageId(id);
        if (contentCount == 0) {
            throw new UnprocessableEntityException("Для публикации в пакете должен быть минимум 1 элемент контента");
        }

        if (pkg.getTitle() == null || pkg.getTitle().isBlank()) {
            throw new UnprocessableEntityException("Для публикации у пакета должен быть непустой заголовок");
        }

        if (pkg.getPublishedAt() == null) {
            pkg.setPublishedAt(Instant.now());
        }
        pkg.setVersion(pkg.getVersion() + 1);
        pkg.setStatus(HomebrewStatus.PUBLISHED);
        pkg = packageRepository.save(pkg);
        log.info("Homebrew package published: id={}, version={}, by={}", id, pkg.getVersion(), username);
        return toDetailResponse(pkg);
    }

    @Transactional
    public HomebrewDetailResponse unpublish(UUID id, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(id, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (pkg.getStatus() != HomebrewStatus.PUBLISHED) {
            throw new BadRequestException("Снять с публикации можно только опубликованные пакеты");
        }

        pkg.setStatus(HomebrewStatus.DRAFT);
        pkg = packageRepository.save(pkg);
        log.info("Homebrew package unpublished: id={}, by={}", id, username);
        return toDetailResponse(pkg);
    }

    @Transactional
    public Map<String, Object> softDelete(UUID id, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(id, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        long installCount = gmLibraryRepository.countByPackageId(id);

        pkg.setDeletedAt(Instant.now());
        pkg.setDeletedBy(gm);
        packageRepository.save(pkg);
        log.info("Homebrew package soft-deleted: id={}, installationCount={}, by={}", id, installCount, username);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Пакет удален");
        result.put("installationCount", installCount);
        return result;
    }

    private HomebrewPackage getEditablePackage(UUID packageId, User gm) {
        HomebrewPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));
        if (!pkg.getAuthor().getId().equals(gm.getId())) {
            throw new AccessDeniedException("Нельзя создавать контент в чужом homebrew-пакете");
        }
        if (pkg.isDeleted() || pkg.getStatus() != HomebrewStatus.DRAFT) {
            throw new BadRequestException("Контент можно создавать только в DRAFT-пакете");
        }
        return pkg;
    }

    private void attachContentItem(HomebrewPackage pkg, ContentType contentType, UUID contentId, String username) {
        UUID packageId = pkg.getId();
        if (contentItemRepository.existsByHomebrewPackageIdAndContentTypeAndContentId(packageId, contentType, contentId)) {
            throw new DuplicateResourceException("Этот контент уже есть в пакете");
        }

        HomebrewContentItem item = HomebrewContentItem.builder()
                .homebrewPackage(pkg)
                .contentType(contentType)
                .contentId(contentId)
                .build();
        contentItemRepository.save(item);
        log.info("Homebrew authoring content created: packageId={}, contentType={}, contentId={}, author={}",
                packageId, contentType, contentId, username);
    }

    private EquipmentSlot parseSlot(String slot, HomebrewPackage pkg) {
        if (slot == null) {
            throw new BadRequestException("Некорректный слот экипировки: null");
        }
        return contentDictionaryResolver.resolveEquipmentSlot(slot, pkg);
    }

    private DamageType parseDamageType(String damageType, HomebrewPackage pkg) {
        return contentDictionaryResolver.resolveDamageType(damageType, pkg);
    }

    private SkillActivation parseSkillActivation(String activation) {
        if (activation == null) {
            return null;
        }
        try {
            return SkillActivation.valueOf(activation);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Некорректный тип активации: " + activation + ". Допустимо: PASSIVE, ACTIVE");
        }
    }

    private void validateDamageFields(String damageDice, String damageType, HomebrewPackage pkg) {
        if (damageDice != null && damageType == null) {
            throw new BadRequestException("Если указан damageDice, damageType обязателен");
        }
        if (damageType != null) {
            parseDamageType(damageType, pkg);
        }
    }

    private void validateItemTypeSkillFields(UUID skillId, String skillActivation) {
        if (skillId != null && skillActivation == null) {
            throw new BadRequestException("Если указан skillId, skillActivation обязателен");
        }
        if (skillActivation != null && skillId == null) {
            throw new BadRequestException("Если указан skillActivation, skillId обязателен");
        }
        if (skillActivation != null) {
            parseSkillActivation(skillActivation);
        }
    }

    private Skill getAllowedPackageSkill(UUID skillId, HomebrewPackage pkg) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Умение не найдено"));
        HomebrewPackage skillPackage = skill.getHomebrew();
        if (skillPackage != null && !skillPackage.getId().equals(pkg.getId())) {
            throw new AccessDeniedException("Можно привязать только vanilla-умение или умение из этого же пакета");
        }
        return skill;
    }

    private BuffDebuff createBuffDebuffInPackage(HomebrewPackage pkg, CreateBuffDebuffRequest request, String username) {
        if (buffDebuffRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException("Бафф/дебафф с таким названием уже существует");
        }
        if ("STAT_MODIFIER".equals(request.getEffectType()) && request.getTargetStatId() == null) {
            throw new BadRequestException("Для эффекта STAT_MODIFIER нужно указать targetStatId");
        }
        BuffDebuff buffDebuff = BuffDebuff.builder()
                .name(request.getName())
                .description(request.getDescription())
                .effectType(request.getEffectType())
                .modifierValue(request.getModifierValue())
                .durationRounds(request.getDurationRounds())
                .isBuff(request.getIsBuff())
                .homebrew(pkg)
                .build();
        if (request.getTargetStatId() != null) {
            StatType statType = statTypeRepository.findById(request.getTargetStatId())
                    .orElseThrow(() -> new ResourceNotFoundException("Тип характеристики не найден: " + request.getTargetStatId()));
            requireVanillaOrSamePackage(pkg, statType.getHomebrew(), "Характеристика");
            buffDebuff.setTargetStat(statType);
        }
        BuffDebuff saved = buffDebuffRepository.save(buffDebuff);
        if (pkg != null) {
            attachContentItem(pkg, ContentType.BUFF_DEBUFF, saved.getId(), username);
        }
        return saved;
    }

    private void requireVanillaOrSamePackage(HomebrewPackage expected, HomebrewPackage actual, String contentLabel) {
        if (expected == null) {
            if (actual != null) {
                throw new AccessDeniedException(contentLabel + " должен быть стандартным, без homebrew-пакета");
            }
            return;
        }
        if (actual != null && !actual.getId().equals(expected.getId())) {
            throw new AccessDeniedException(contentLabel + " должен быть vanilla или из этого же homebrew-пакета");
        }
    }

    // --- Mapping helpers ---

    HomebrewPackageResponse toPackageResponse(HomebrewPackage pkg) {
        String title = pkg.getTitle();
        if (pkg.isDeleted()) {
            title = "[УДАЛЕНО] " + title;
        }

        return HomebrewPackageResponse.builder()
                .id(pkg.getId())
                .title(title)
                .description(pkg.getDescription())
                .status(pkg.getStatus().name())
                .version(pkg.getVersion())
                .downloadCount(pkg.getDownloadCount())
                .authorUsername(pkg.getAuthor().getUsername())
                .tags(pkg.getTags().stream().map(HomebrewTag::getName).sorted().toList())
                .contentSummary(buildContentSummary(pkg.getId()))
                .publishedAt(pkg.getPublishedAt())
                .createdAt(pkg.getCreatedAt())
                .isDeleted(pkg.isDeleted())
                .build();
    }

    HomebrewDetailResponse toDetailResponse(HomebrewPackage pkg) {
        String title = pkg.getTitle();
        if (pkg.isDeleted()) {
            title = "[УДАЛЕНО] " + title;
        }

        return HomebrewDetailResponse.builder()
                .id(pkg.getId())
                .title(title)
                .description(pkg.getDescription())
                .status(pkg.getStatus().name())
                .version(pkg.getVersion())
                .downloadCount(pkg.getDownloadCount())
                .authorUsername(pkg.getAuthor().getUsername())
                .tags(pkg.getTags().stream().map(HomebrewTag::getName).sorted().toList())
                .contentSummary(buildContentSummary(pkg.getId()))
                .contentByType(buildContentByType(pkg.getId()))
                .publishedAt(pkg.getPublishedAt())
                .createdAt(pkg.getCreatedAt())
                .isDeleted(pkg.isDeleted())
                .build();
    }

    HomebrewContentSummary buildContentSummary(UUID packageId) {
        List<Object[]> counts = contentItemRepository.countByPackageGroupedByType(packageId);
        HomebrewContentSummary summary = new HomebrewContentSummary();
        for (Object[] row : counts) {
            ContentType type = (ContentType) row[0];
            int count = ((Number) row[1]).intValue();
            switch (type) {
                case ITEM_TYPE -> summary.setItemTypeCount(count);
                case CHARACTER_CLASS -> summary.setClassCount(count);
                case RACE -> summary.setRaceCount(count);
                case SKILL -> summary.setSkillCount(count);
                case FEAT -> summary.setFeatCount(count);
                case SUBCLASS -> summary.setSubclassCount(count);
                case BUFF_DEBUFF -> summary.setBuffDebuffCount(count);
            }
        }
        return summary;
    }

    private Map<String, List<ContentSummaryDto>> buildContentByType(UUID packageId) {
        List<HomebrewContentItem> items = contentItemRepository.findAllByHomebrewPackageId(packageId);
        Map<String, List<ContentSummaryDto>> result = new LinkedHashMap<>();

        Map<String, List<HomebrewContentItem>> grouped = items.stream()
                .collect(Collectors.groupingBy(i -> i.getContentType().name()));

        for (Map.Entry<String, List<HomebrewContentItem>> entry : grouped.entrySet()) {
            List<ContentSummaryDto> summaries = entry.getValue().stream()
                    .map(item -> validatorRegistry.summarize(item.getContentType().name(), item.getContentId()))
                    .toList();
            result.put(entry.getKey(), summaries);
        }
        return result;
    }

    private User getGameMaster(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (user.getRole() != Role.GAME_MASTER && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только мастера игры могут управлять homebrew-пакетами");
        }
        return user;
    }

    private User getRequiredGameMaster(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (user.getRole() != Role.GAME_MASTER) {
            throw new AccessDeniedException("Только мастера игры могут создавать контент homebrew-пакетов");
        }
        return user;
    }

    private HomebrewStatus parseStatus(String status) {
        try {
            return HomebrewStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Некорректный статус: " + status);
        }
    }
}
