package com.dnd.app.service.homebrew;

import com.dnd.app.domain.HomebrewInstallation;
import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.response.*;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.HomebrewInstallationRepository;
import com.dnd.app.repository.HomebrewPackageRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class HomebrewMarketplaceService {

    private final HomebrewPackageRepository packageRepository;
    private final HomebrewInstallationRepository installationRepository;
    private final UserRepository userRepository;
    private final HomebrewAuthoringService authoringService;

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

    @Transactional(readOnly = true)
    public HomebrewDetailResponse getMarketplacePackage(UUID id, String username) {
        getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findPublishedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));
        return authoringService.toDetailResponse(pkg);
    }

    @Transactional
    public Map<String, Object> installPackage(UUID packageId, String username) {
        User gm = getGameMaster(username);
        HomebrewPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));

        if (pkg.getStatus() != HomebrewStatus.PUBLISHED || pkg.isDeleted()) {
            throw new ResourceNotFoundException("Пакет не найден");
        }

        if (installationRepository.existsByHomebrewPackageIdAndInstallerId(packageId, gm.getId())) {
            throw new DuplicateResourceException("Пакет уже установлен");
        }

        HomebrewInstallation installation = HomebrewInstallation.builder()
                .homebrewPackage(pkg)
                .installer(gm)
                .sourceVersion(pkg.getVersion())
                .build();
        installationRepository.save(installation);

        pkg.setDownloadCount(pkg.getDownloadCount() + 1);
        packageRepository.save(pkg);

        long contentCount = authoringService.buildContentSummary(packageId).getItemTypeCount()
                + authoringService.buildContentSummary(packageId).getClassCount()
                + authoringService.buildContentSummary(packageId).getSkillCount()
                + authoringService.buildContentSummary(packageId).getFeatCount();

        log.info("Package installed: packageId={}, version={}, by={}", packageId, pkg.getVersion(), username);

        Map<String, Object> result = new HashMap<>();
        result.put("installedAt", installation.getInstalledAt());
        result.put("sourceVersion", installation.getSourceVersion());
        result.put("contentCount", contentCount);
        return result;
    }

    @Transactional(readOnly = true)
    public Page<InstalledHomebrewResponse> listInstalled(String username, Pageable pageable) {
        User gm = getGameMaster(username);
        Page<HomebrewInstallation> installations = installationRepository.findAllByInstallerId(gm.getId(), pageable);

        return installations.map(inst -> {
            HomebrewPackage pkg = inst.getHomebrewPackage();
            String title = pkg.getTitle();
            if (pkg.isDeleted()) {
                title = com.dnd.app.util.ResponseLocalizer.deletedTitle(title);
            }
            return InstalledHomebrewResponse.builder()
                    .installationId(inst.getId())
                    .packageId(pkg.getId())
                    .title(title)
                    .authorUsername(pkg.getAuthor().getUsername())
                    .isDeleted(pkg.isDeleted())
                    .installedAt(inst.getInstalledAt())
                    .sourceVersion(inst.getSourceVersion())
                    .contentSummary(authoringService.buildContentSummary(pkg.getId()))
                    .build();
        });
    }

    @Transactional
    public void uninstall(UUID installationId, String username) {
        User gm = getGameMaster(username);
        HomebrewInstallation installation = installationRepository.findByIdAndInstallerId(installationId, gm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Установка не найдена"));
        installationRepository.delete(installation);
        log.info("Package uninstalled: installationId={}, packageId={}, by={}",
                installationId, installation.getHomebrewPackage().getId(), username);
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
