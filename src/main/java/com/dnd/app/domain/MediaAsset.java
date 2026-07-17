package com.dnd.app.domain;

import com.dnd.app.domain.enums.MediaOwnerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс MediaAsset описывает доменную модель одной загруженной картинки, хранящейся в MinIO.
 * Это полиморфная сущность: пара {@code ownerType} + {@code ownerId} адресует владельца
 * (персонаж, NPC, homebrew-пакет, blueprint и т.д.), а на пару наложено ограничение
 * уникальности — один слот картинки на владельца. Идентификатор задаётся сервисом вручную
 * (без {@code @GeneratedValue}), потому что он нужен ещё до вставки — из него строится
 * {@code storageKey} объекта в бакете.
 */
@Entity
@Table(name = "media_asset", indexes = {
        @Index(name = "idx_media_asset_uploaded_by", columnList = "uploaded_by"),
        @Index(name = "idx_media_asset_owner", columnList = "owner_type, owner_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaAsset {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, columnDefinition = "text")
    private MediaOwnerType ownerType;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "bucket_name", nullable = false, columnDefinition = "text")
    private String bucketName;

    @Column(name = "storage_key", nullable = false, columnDefinition = "text")
    private String storageKey;

    @Column(name = "original_filename", nullable = false, columnDefinition = "text")
    private String originalFilename;

    @Column(name = "content_type", nullable = false, columnDefinition = "text")
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "width_px")
    private Integer widthPx;

    @Column(name = "height_px")
    private Integer heightPx;

    @Column(name = "checksum_sha256", columnDefinition = "text")
    private String checksumSha256;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
