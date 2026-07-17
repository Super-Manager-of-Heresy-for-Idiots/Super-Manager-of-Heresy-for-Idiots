package com.dnd.app.service.media;

import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.exception.AccessDeniedException;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.HomebrewPackageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс HomebrewCoverPolicy — политика прав на обложку homebrew-пакета ({@code HOMEBREW_COVER}).
 * Загрузку/замену/удаление разрешает автору пакета (или ADMIN) и только пока пакет редактируем
 * ({@code HomebrewStatus.isEditable()} — DRAFT/PUBLISHED, не удалён); удаление ADMIN'ом любого ассета
 * дополнительно поддержано в {@code MediaService} (модерация витрины). Чтение — любому
 * аутентифицированному: обложки показываются на публичной витрине.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.endpoint")
public class HomebrewCoverPolicy implements MediaOwnerPolicy {

    private final HomebrewPackageRepository packageRepository;

    /** @return тип владельца, обслуживаемый политикой — обложка homebrew-пакета */
    @Override
    public MediaOwnerType type() {
        return MediaOwnerType.HOMEBREW_COVER;
    }

    /**
     * Разрешает менять обложку автору пакета (или ADMIN) в редактируемом статусе.
     * @param ownerId идентификатор homebrew-пакета
     * @param user текущий пользователь
     */
    @Override
    public void checkUpload(UUID ownerId, MediaUser user) {
        HomebrewPackage pkg = requirePackage(ownerId);
        boolean ownerOrAdmin = user.isAdmin()
                || (pkg.getAuthor() != null && pkg.getAuthor().getId().equals(user.id()));
        if (!ownerOrAdmin) {
            throw new AccessDeniedException("Обложку пакета может менять только его автор.");
        }
        if (pkg.isDeleted() || !pkg.getStatus().isEditable()) {
            throw new BadRequestException("Обложку можно менять только в редактируемом пакете (DRAFT/PUBLISHED).");
        }
    }

    /**
     * Разрешает чтение обложки любому аутентифицированному пользователю (витрина).
     * @param ownerId идентификатор homebrew-пакета
     * @param user текущий пользователь
     */
    @Override
    public void checkRead(UUID ownerId, MediaUser user) {
        // Обложка витрины видна любому аутентифицированному пользователю — дополнительных проверок нет.
    }

    /**
     * Находит пакет или бросает 404.
     * @param id идентификатор homebrew-пакета
     * @return сущность homebrew-пакета
     */
    private HomebrewPackage requirePackage(UUID id) {
        return packageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Homebrew-пакет не найден."));
    }
}
