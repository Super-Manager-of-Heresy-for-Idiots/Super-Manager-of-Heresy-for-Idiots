package com.dnd.app.service.homebrew;

import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.HomebrewPackageRepository;
import com.dnd.app.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Класс HomebrewAccessService описывает единый guard доступа к homebrew-пакетам (P0-1).
 * Централизует правило разделения доступа: владелец или ADMIN получают полный доступ к пакету
 * в любом статусе, а чужой пакет можно только читать и только если он опубликован (PUBLISHED)
 * и не удалён. Любой эндпоинт вида /homebrew/{packageId}/* обязан проходить через этот guard,
 * чтобы черновики и приватный контент не утекали к посторонним аутентифицированным пользователям.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HomebrewAccessService {

    private final HomebrewPackageRepository packageRepository;
    private final UserRepository userRepository;

    /**
     * Проверяет право читать пакет и возвращает его.
     * Владелец и ADMIN читают пакет в любом статусе; посторонний — только опубликованный неудалённый.
     * @param packageId идентификатор homebrew-пакета
     * @param username имя пользователя, от имени которого выполняется доступ
     * @return найденный и доступный на чтение пакет
     */
    @Transactional(readOnly = true)
    public HomebrewPackage enforceReadable(UUID packageId, String username) {
        return enforceReadable(packageId, getUser(username));
    }

    /**
     * Проверяет право читать пакет и возвращает его.
     * @param packageId идентификатор homebrew-пакета
     * @param user пользователь, от имени которого выполняется доступ
     * @return найденный и доступный на чтение пакет
     */
    @Transactional(readOnly = true)
    public HomebrewPackage enforceReadable(UUID packageId, User user) {
        HomebrewPackage pkg = getPackage(packageId);
        if (isOwnerOrAdmin(pkg, user)) {
            return pkg;
        }
        if (pkg.getStatus() == HomebrewStatus.PUBLISHED && !pkg.isDeleted()) {
            return pkg;
        }
        log.warn("Homebrew access denied (read): packageId={}, user={}, status={}, deleted={}",
                packageId, user.getUsername(), pkg.getStatus(), pkg.isDeleted());
        throw new AccessDeniedException("Нет доступа к этому homebrew-пакету");
    }

    /**
     * Проверяет право управлять пакетом (владелец или ADMIN) и возвращает его.
     * @param packageId идентификатор homebrew-пакета
     * @param username имя пользователя, от имени которого выполняется доступ
     * @return найденный пакет, которым пользователь вправе владеть/управлять
     */
    @Transactional(readOnly = true)
    public HomebrewPackage enforceOwner(UUID packageId, String username) {
        return enforceOwner(packageId, getUser(username));
    }

    /**
     * Проверяет право управлять пакетом (владелец или ADMIN) и возвращает его.
     * @param packageId идентификатор homebrew-пакета
     * @param user пользователь, от имени которого выполняется доступ
     * @return найденный пакет, которым пользователь вправе владеть/управлять
     */
    @Transactional(readOnly = true)
    public HomebrewPackage enforceOwner(UUID packageId, User user) {
        HomebrewPackage pkg = getPackage(packageId);
        if (!isOwnerOrAdmin(pkg, user)) {
            log.warn("Homebrew access denied (owner): packageId={}, user={}", packageId, user.getUsername());
            throw new AccessDeniedException("Этот homebrew-пакет вам не принадлежит");
        }
        return pkg;
    }

    /**
     * Проверяет, является ли пользователь владельцем пакета или администратором.
     * @param pkg homebrew-пакет
     * @param user пользователь
     * @return true, если владелец или ADMIN
     */
    public boolean isOwnerOrAdmin(HomebrewPackage pkg, User user) {
        return user.getRole() == Role.ADMIN
                || (pkg.getAuthor() != null && pkg.getAuthor().getId().equals(user.getId()));
    }

    private HomebrewPackage getPackage(UUID packageId) {
        return packageRepository.findById(packageId)
                .orElseThrow(() -> new ResourceNotFoundException("Пакет не найден"));
    }

    private User getUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Пользователь не найден"));
    }
}
