package com.dnd.app.service.media;

import com.dnd.app.config.MediaProperties;
import com.dnd.app.domain.MediaAsset;
import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс BlueprintCoverForkCopier копирует обложку-ассет при форке blueprint (Фаза 4).
 * Вынесен в отдельный условный бин ({@code @ConditionalOnProperty minio.endpoint}), чтобы сервис форка
 * не зависел напрямую от MinIO: при выключенном media-модуле бин отсутствует и вызывающий код делает
 * no-op (легаси-поле {@code cover_url} у форка уже скопировано отдельно — корректный fallback).
 * Копирует объект в хранилище и создаёт новую запись {@code media_asset} для форка (владелец —
 * форкающий пользователь). Выполняется внутри транзакции форка.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "minio.endpoint")
public class BlueprintCoverForkCopier {

    private final MediaAssetRepository repository;
    private final AssetStorage storage;
    private final MediaStorageKeys storageKeys;
    private final MediaProperties properties;

    /**
     * Копирует обложку с исходного blueprint на форк, если она есть.
     * @param sourceBlueprintId идентификатор исходного (родительского) blueprint
     * @param targetBlueprintId идентификатор нового blueprint-форка
     * @param newOwnerUserId идентификатор пользователя-форкающего (владелец новой записи ассета)
     */
    public void copyCover(UUID sourceBlueprintId, UUID targetBlueprintId, UUID newOwnerUserId) {
        repository.findByOwnerTypeAndOwnerId(MediaOwnerType.BLUEPRINT_COVER, sourceBlueprintId)
                .ifPresent(source -> {
                    UUID newAssetId = UUID.randomUUID();
                    String bucket = properties.getBucket();
                    String newStorageKey = storageKeys.objectKey(
                            MediaOwnerType.BLUEPRINT_COVER, targetBlueprintId, newAssetId, source.getOriginalFilename());

                    storage.copy(source.getBucketName(), source.getStorageKey(), bucket, newStorageKey);

                    MediaAsset copy = MediaAsset.builder()
                            .id(newAssetId)
                            .ownerType(MediaOwnerType.BLUEPRINT_COVER)
                            .ownerId(targetBlueprintId)
                            .bucketName(bucket)
                            .storageKey(newStorageKey)
                            .originalFilename(source.getOriginalFilename())
                            .contentType(source.getContentType())
                            .sizeBytes(source.getSizeBytes())
                            .widthPx(source.getWidthPx())
                            .heightPx(source.getHeightPx())
                            .checksumSha256(source.getChecksumSha256())
                            .uploadedBy(newOwnerUserId)
                            .build();
                    repository.save(copy);
                });
    }
}
