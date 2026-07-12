package com.dnd.app.service.homebrew;

import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.User;
import com.dnd.app.domain.enums.HomebrewStatus;
import com.dnd.app.domain.enums.Role;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.repository.HomebrewPackageRepository;
import com.dnd.app.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Тест HomebrewAccessServiceTest проверяет единый guard доступа к homebrew-пакетам (P0-1):
 * владелец/ADMIN — полный доступ; чужой пакет — только чтение и только если он опубликован.
 */
@ExtendWith(MockitoExtension.class)
class HomebrewAccessServiceTest {

    @Mock private HomebrewPackageRepository packageRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private HomebrewAccessService service;

    private final UUID packageId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();
    private final UUID strangerId = UUID.randomUUID();

    private User user(UUID id, Role role) {
        User u = new User();
        u.setId(id);
        u.setUsername("u-" + id);
        u.setRole(role);
        return u;
    }

    private HomebrewPackage pkg(HomebrewStatus status, boolean deleted) {
        User author = user(ownerId, Role.GAME_MASTER);
        HomebrewPackage p = HomebrewPackage.builder()
                .author(author)
                .title("Pkg")
                .status(status)
                .build();
        p.setId(packageId);
        if (deleted) {
            p.setDeletedAt(java.time.Instant.now());
        }
        return p;
    }

    private void stub(User u, HomebrewPackage p) {
        lenient().when(userRepository.findByUsername(u.getUsername())).thenReturn(Optional.of(u));
        lenient().when(packageRepository.findById(packageId)).thenReturn(Optional.of(p));
    }

    @Test
    @DisplayName("enforceReadable: владелец читает свой черновик")
    void enforceReadable_owner_ok() {
        User owner = user(ownerId, Role.GAME_MASTER);
        HomebrewPackage p = pkg(HomebrewStatus.DRAFT, false);
        stub(owner, p);
        assertSame(p, service.enforceReadable(packageId, owner.getUsername()));
    }

    @Test
    @DisplayName("enforceReadable: ADMIN читает чужой черновик")
    void enforceReadable_admin_ok() {
        User admin = user(strangerId, Role.ADMIN);
        HomebrewPackage p = pkg(HomebrewStatus.DRAFT, false);
        stub(admin, p);
        assertSame(p, service.enforceReadable(packageId, admin.getUsername()));
    }

    @Test
    @DisplayName("enforceReadable: посторонний читает опубликованный пакет")
    void enforceReadable_publishedStranger_ok() {
        User stranger = user(strangerId, Role.GAME_MASTER);
        HomebrewPackage p = pkg(HomebrewStatus.PUBLISHED, false);
        stub(stranger, p);
        assertSame(p, service.enforceReadable(packageId, stranger.getUsername()));
    }

    @Test
    @DisplayName("enforceReadable: посторонний НЕ читает чужой черновик")
    void enforceReadable_draftStranger_denied() {
        User stranger = user(strangerId, Role.GAME_MASTER);
        HomebrewPackage p = pkg(HomebrewStatus.DRAFT, false);
        stub(stranger, p);
        assertThrows(AccessDeniedException.class,
                () -> service.enforceReadable(packageId, stranger.getUsername()));
    }

    @Test
    @DisplayName("enforceReadable: посторонний НЕ читает удалённый опубликованный пакет")
    void enforceReadable_deletedPublishedStranger_denied() {
        User stranger = user(strangerId, Role.GAME_MASTER);
        HomebrewPackage p = pkg(HomebrewStatus.PUBLISHED, true);
        stub(stranger, p);
        assertThrows(AccessDeniedException.class,
                () -> service.enforceReadable(packageId, stranger.getUsername()));
    }

    @Test
    @DisplayName("enforceOwner: владелец проходит")
    void enforceOwner_owner_ok() {
        User owner = user(ownerId, Role.GAME_MASTER);
        HomebrewPackage p = pkg(HomebrewStatus.PUBLISHED, false);
        stub(owner, p);
        assertSame(p, service.enforceOwner(packageId, owner.getUsername()));
    }

    @Test
    @DisplayName("enforceOwner: посторонний получает отказ даже на опубликованном пакете")
    void enforceOwner_stranger_denied() {
        User stranger = user(strangerId, Role.GAME_MASTER);
        HomebrewPackage p = pkg(HomebrewStatus.PUBLISHED, false);
        stub(stranger, p);
        assertThrows(AccessDeniedException.class,
                () -> service.enforceOwner(packageId, stranger.getUsername()));
    }
}
