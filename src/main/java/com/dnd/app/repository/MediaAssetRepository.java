package com.dnd.app.repository;

import com.dnd.app.domain.MediaAsset;
import com.dnd.app.domain.enums.MediaOwnerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий MediaAssetRepository предоставляет доступ к записям таблицы media_asset.
 * Ключевые операции: поиск ассета по слоту владельца (owner_type + owner_id) и подсчёт
 * суммарного размера всех ассетов пользователя для проверки квоты.
 */
public interface MediaAssetRepository extends JpaRepository<MediaAsset, UUID> {

    /**
     * Находит ассет, занимающий слот указанного владельца.
     * @param ownerType тип владельца ассета
     * @param ownerId идентификатор владельца ассета
     * @return ассет слота, если он существует
     */
    Optional<MediaAsset> findByOwnerTypeAndOwnerId(MediaOwnerType ownerType, UUID ownerId);

    /**
     * Считает суммарный размер (в байтах) всех ассетов, загруженных пользователем.
     * COALESCE возвращает 0, когда у пользователя ещё нет ни одного ассета.
     * @param userId идентификатор пользователя, чьи ассеты суммируются
     * @return суммарный размер ассетов пользователя в байтах (0, если ассетов нет)
     */
    @Query("SELECT COALESCE(SUM(m.sizeBytes), 0) FROM MediaAsset m WHERE m.uploadedBy = :userId")
    long sumSizeBytesByUploadedBy(@Param("userId") UUID userId);
}
