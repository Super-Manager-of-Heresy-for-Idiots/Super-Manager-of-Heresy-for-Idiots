package com.dnd.app.service;

import com.dnd.app.domain.*;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.dto.response.HomebrewPackageResponse;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.DuplicateResourceException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.GmHomebrewLibraryRepository;
import com.dnd.app.repository.HomebrewPackageRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Класс HomebrewLibraryService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomebrewLibraryService {

    private final GmHomebrewLibraryRepository libraryRepository;
    private final HomebrewPackageRepository packageRepository;
    private final UserRepository userRepository;

    /**
     * Возвращает список для операции "list library" в рамках бизнес-логики домена.
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     * @return результат выполнения бизнес-операции
     */
    @Transactional(readOnly = true)
    public List<HomebrewPackageResponse> listLibrary(String username) {
        User user = getUser(username);
        enforceGmOrAdmin(user);

        return libraryRepository.findByGmUserId(user.getId()).stream()
                .map(entry -> toPackageResponse(entry.getHomebrewPackage()))
                .toList();
    }

    /**
     * Добавляет результат операции "add to library" в рамках бизнес-логики домена.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void addToLibrary(UUID packageId, String username) {
        User user = getUser(username);
        enforceGmOrAdmin(user);

        HomebrewPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Homebrew package not found"));

        if (pkg.getStatus() != HomebrewStatus.PUBLISHED) {
            throw new BadRequestException("Can only add published packages to library");
        }

        if (libraryRepository.existsByGmUserIdAndPackageId(user.getId(), packageId)) {
            throw new DuplicateResourceException("Package is already in your library");
        }

        GmHomebrewLibrary entry = GmHomebrewLibrary.builder()
                .gmUserId(user.getId())
                .packageId(packageId)
                .build();
        libraryRepository.save(entry);

        log.info("Package added to GM library: packageId={}, gmUser={}", packageId, username);
    }

    /**
     * Удаляет результат операции "remove from library" в рамках бизнес-логики домена.
     * @param packageId идентификатор package, используемый для выбора нужного бизнес-объекта
     * @param username имя пользователя, от имени которого выполняется бизнес-сценарий
     */
    @Transactional
    public void removeFromLibrary(UUID packageId, String username) {
        User user = getUser(username);
        enforceGmOrAdmin(user);

        if (!libraryRepository.existsByGmUserIdAndPackageId(user.getId(), packageId)) {
            throw new ResourceNotFoundException("Package not found in your library");
        }

        libraryRepository.deleteByGmUserIdAndPackageId(user.getId(), packageId);
        log.info("Package removed from GM library: packageId={}, gmUser={}", packageId, username);
    }

    // --- Private helpers ---

    private void enforceGmOrAdmin(User user) {
        if (user.getRole() != Role.GAME_MASTER && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only Game Masters and Admins can manage the homebrew library");
        }
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private HomebrewPackageResponse toPackageResponse(HomebrewPackage pkg) {
        return HomebrewPackageResponse.builder()
                .id(pkg.getId())
                .title(pkg.getTitle())
                .description(pkg.getDescription())
                .status(pkg.getStatus().name())
                .version(pkg.getVersion())
                .authorUsername(pkg.getAuthor() != null ? pkg.getAuthor().getUsername() : null)
                .createdAt(pkg.getCreatedAt())
                .isDeleted(pkg.isDeleted())
                .build();
    }
}
