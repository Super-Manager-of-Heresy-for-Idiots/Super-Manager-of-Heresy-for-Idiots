package com.dnd.app.service.homebrew;

import com.dnd.app.domain.GmHomebrewLibrary;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.HomebrewRating;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.request.RateHomebrewRequest;
import com.dnd.app.dto.response.*;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.GmHomebrewLibraryRepository;
import com.dnd.app.repository.HomebrewPackageRepository;
import com.dnd.app.repository.HomebrewRatingRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Класс HomebrewMarketplaceService описывает сервис homebrew-логики, который проверяет и обслуживает пользовательский контент.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomebrewMarketplaceService {

    private final HomebrewPackageRepository packageRepository;
    private final GmHomebrewLibraryRepository gmLibraryRepository;
    private final HomebrewRatingRepository ratingRepository;
    private final UserRepository userRepository;
    private final HomebrewAuthoringService authoringService;

    /**
     * Выполняет операции "browse marketplace" в рамках бизнес-логики homebrew-контента.
     * @param search входящее значение search, используемое бизнес-сценарием
     * @param tags входящее значение tags, используемое бизнес-сценарием
     * @param sort входящее значение sort, используемое бизнес-сценарием
     * @param page входящее значение page, используемое бизнес-сценарием
     * @param size входящее значение size, используемое бизнес-сценарием
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public Page<HomebrewPackageResponse> browseMarketplace(String search, List<String> tags,
                                                            String sort, int page, int size,
                                                            String username) {
        getGameMaster(username);
        Pageable pageable = buildPageable(sort, page, size);

        Page<HomebrewPackage> packages;
        boolean hasTags = tags != null && !tags.isEmpty();
        boolean hasSearch = search != null && !search.isBlank();

        if (hasTags && hasSearch) {
            packages = packageRepository.findPublishedByAllTagsAndSearch(tags, tags.size(), search, pageable);
        } else if (hasTags) {
            packages = packageRepository.findPublishedByAllTags(tags, tags.size(), pageable);
        } else if (hasSearch) {
            packages = packageRepository.findPublishedBySearch(search, pageable);
        } else {
            packages = packageRepository.findPublishedAndNotDeleted(pageable);
        }

        return packages.map(authoringService::toPackageResponse);
    }

    /**
     * Возвращает результат операции "get marketplace package" в рамках бизнес-логики homebrew-контента.
     * @param id идентификатор id, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public HomebrewDetailResponse getMarketplacePackage(UUID id, String username) {
        getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findPublishedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));
        return authoringService.toDetailResponse(pkg);
    }

    /**
     * Выполняет операции "install package" в рамках бизнес-логики homebrew-контента.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public Map<String, Object> installPackage(UUID packageId, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (pkg.getStatus() != HomebrewStatus.PUBLISHED || pkg.isDeleted()) {
            throw new ResourceNotFoundException("Пакет не найден");
        }

        if (gmLibraryRepository.existsByGmUserIdAndPackageId(gm.getId(), packageId)) {
            throw new DuplicateResourceException("Пакет уже добавлен в библиотеку");
        }

        GmHomebrewLibrary entry = GmHomebrewLibrary.builder()
                .gmUserId(gm.getId())
                .packageId(packageId)
                .build();
        gmLibraryRepository.save(entry);

        pkg.setDownloadCount(pkg.getDownloadCount() + 1);
        packageRepository.save(pkg);

        log.info("Package added to library: packageId={}, version={}, by={}", packageId, pkg.getVersion(), username);

        Map<String, Object> result = new HashMap<>();
        result.put("addedAt", entry.getAddedAt());
        result.put("packageVersion", pkg.getVersion());
        return result;
    }

    /**
     * Возвращает список для операции "list installed" в рамках бизнес-логики homebrew-контента.
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @param pageable параметры постраничной выдачи для бизнес-списка
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public Page<InstalledHomebrewResponse> listInstalled(String username, Pageable pageable) {
        User gm = getGameMaster(username);
        List<GmHomebrewLibrary> entries = gmLibraryRepository.findByGmUserId(gm.getId());

        List<InstalledHomebrewResponse> responses = entries.stream().map(entry -> {
            HomebrewPackage pkg = entry.getHomebrewPackage();
            String title = pkg.getTitle();
            if (pkg.isDeleted()) {
                title = "[УДАЛЕНО] " + title;
            }
            return InstalledHomebrewResponse.builder()
                    .packageId(pkg.getId())
                    .title(title)
                    .authorUsername(pkg.getAuthor().getUsername())
                    .isDeleted(pkg.isDeleted())
                    .installedAt(entry.getAddedAt())
                    .sourceVersion(pkg.getVersion())
                    .contentSummary(authoringService.buildContentSummary(pkg.getId()))
                    .build();
        }).toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), responses.size());
        if (start > responses.size()) {
            start = responses.size();
        }
        return new org.springframework.data.domain.PageImpl<>(
                responses.subList(start, end), pageable, responses.size());
    }

    /**
     * Выполняет операции "uninstall" в рамках бизнес-логики homebrew-контента.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void uninstall(UUID packageId, String username) {
        User gm = getGameMaster(username);
        if (!gmLibraryRepository.existsByGmUserIdAndPackageId(gm.getId(), packageId)) {
            throw new ResourceNotFoundException("Пакет не найден в библиотеке");
        }
        gmLibraryRepository.deleteByGmUserIdAndPackageId(gm.getId(), packageId);
        log.info("Package removed from library: packageId={}, by={}", packageId, username);
    }

    /**
     * Выполняет операции "rate package" в рамках бизнес-логики homebrew-контента.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param request входящие данные запроса для выполнения бизнес-сценария
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional
    public HomebrewRatingResponse ratePackage(UUID packageId, RateHomebrewRequest request, String username) {
        User user = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        if (pkg.getStatus() != HomebrewStatus.PUBLISHED) {
            throw new ResourceNotFoundException("Package not found");
        }

        HomebrewRating rating = ratingRepository.findByUserIdAndPackageId(user.getId(), packageId)
                .orElse(null);

        if (rating != null) {
            rating.setRating(request.getRating());
            ratingRepository.save(rating);
        } else {
            rating = HomebrewRating.builder()
                    .userId(user.getId())
                    .packageId(packageId)
                    .rating(request.getRating())
                    .build();
            ratingRepository.save(rating);
        }

        log.info("Package rated: packageId={}, rating={}, by={}", packageId, request.getRating(), username);
        return buildRatingResponse(packageId, user.getId());
    }

    /**
     * Возвращает результат операции "get package rating" в рамках бизнес-логики homebrew-контента.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public HomebrewRatingResponse getPackageRating(UUID packageId, String username) {
        User user = getGameMaster(username);
        packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Package not found"));

        return buildRatingResponse(packageId, user.getId());
    }

    private HomebrewRatingResponse buildRatingResponse(UUID packageId, UUID userId) {
        long likes = ratingRepository.countByPackageIdAndRating(packageId, 1);
        long dislikes = ratingRepository.countByPackageIdAndRating(packageId, -1);
        Integer userRating = ratingRepository.findByUserIdAndPackageId(userId, packageId)
                .map(HomebrewRating::getRating)
                .orElse(null);

        return HomebrewRatingResponse.builder()
                .likes(likes)
                .dislikes(dislikes)
                .netRating(likes - dislikes)
                .userRating(userRating)
                .build();
    }

    private User getGameMaster(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
        if (user.getRole() != Role.GAME_MASTER && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Только мастера игры могут открывать каталог");
        }
        return user;
    }

    private Pageable buildPageable(String sort, int page, int size) {
        Sort sortOrder = switch (sort != null ? sort.toLowerCase() : "newest") {
            case "downloads" -> Sort.by(Sort.Direction.DESC, "downloadCount");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        return PageRequest.of(page, size, sortOrder);
    }
}
