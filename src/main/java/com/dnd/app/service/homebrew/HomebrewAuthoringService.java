package com.dnd.app.service.homebrew;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.ContentType;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.AddContentRequest;
import com.dnd.app.dto.request.CreateHomebrewRequest;
import com.dnd.app.dto.request.UpdateHomebrewRequest;
import com.dnd.app.dto.response.*;
import com.dnd.app.exception.AccessDeniedException;
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
    private final HomebrewInstallationRepository installationRepository;
    private final UserRepository userRepository;
    private final TagService tagService;
    private final HomebrewContentValidatorRegistry validatorRegistry;

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
            throw new DuplicateResourceException("Пакет можно редактировать только в статусе черновика (DRAFT)");
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
            throw new DuplicateResourceException("Контент можно добавлять только в статусе черновика (DRAFT)");
        }

        String contentTypeStr = request.getContentType();
        if (!validatorRegistry.isKnownType(contentTypeStr)) {
            throw new DuplicateResourceException("Неизвестный тип контента: " + contentTypeStr);
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
    public void removeContent(UUID packageId, UUID contentItemId, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(packageId, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (pkg.getStatus() != HomebrewStatus.DRAFT) {
            throw new DuplicateResourceException("Контент можно удалять только в статусе черновика (DRAFT)");
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

        if (pkg.getStatus() != HomebrewStatus.DRAFT && pkg.getStatus() != HomebrewStatus.UNPUBLISHED) {
            throw new DuplicateResourceException("Пакет можно опубликовать только из статуса черновика (DRAFT) или снятого с публикации (UNPUBLISHED)");
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
            throw new DuplicateResourceException("Снять с публикации можно только опубликованные пакеты");
        }

        pkg.setStatus(HomebrewStatus.UNPUBLISHED);
        pkg = packageRepository.save(pkg);
        log.info("Homebrew package unpublished: id={}, by={}", id, username);
        return toDetailResponse(pkg);
    }

    @Transactional
    public Map<String, Object> softDelete(UUID id, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findByIdAndAuthorId(id, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        long installCount = installationRepository.countByHomebrewPackageId(id);

        pkg.setDeletedAt(Instant.now());
        pkg.setDeletedBy(gm);
        packageRepository.save(pkg);
        log.info("Homebrew package soft-deleted: id={}, installationCount={}, by={}", id, installCount, username);

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Пакет удален");
        result.put("installationCount", installCount);
        return result;
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
                case SKILL -> summary.setSkillCount(count);
                case FEAT -> summary.setFeatCount(count);
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

    private HomebrewStatus parseStatus(String status) {
        try {
            return HomebrewStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.dnd.app.exception.BadRequestException("Некорректный статус: " + status);
        }
    }
}
