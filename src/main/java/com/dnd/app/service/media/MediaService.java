package com.dnd.app.service.media;

import com.dnd.app.config.MediaProperties;
import com.dnd.app.domain.MediaAsset;
import com.dnd.app.domain.enums.MediaLimitCategory;
import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.dto.response.MediaAssetResponse;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.MediaAssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Класс MediaService — ядро media-модуля: загрузка, чтение и удаление картинок с валидацией и
 * контролем прав. Поднимается только при заданном {@code minio.endpoint} (вместе с хранилищем).
 * Загрузка: политика прав → валидация (allowlist типов, размер по категории, реальное декодирование
 * через ImageIO, лимит по пикселям, квота пользователя) → put в MinIO → сохранение записи с заменой
 * слота (один ассет на владельца) → удаление старого объекта после успешного коммита. Сироты не
 * копятся: при падении БД свежезалитый объект удаляется, при успехе — удаляется прежний объект слота.
 */
@Service
@ConditionalOnProperty(name = "minio.endpoint")
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);

    /** Разрешённые MIME-типы загружаемых изображений. */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/png", "image/jpeg", "image/webp");

    private final MediaAssetRepository repository;
    private final AssetStorage storage;
    private final MediaStorageKeys storageKeys;
    private final MediaProperties properties;
    private final MediaPolicyRegistry policyRegistry;

    /**
     * Создаёт сервис media-модуля.
     * @param repository репозиторий записей media_asset
     * @param storage объектное хранилище (MinIO)
     * @param storageKeys генератор ключей объектов
     * @param properties параметры модуля (бакет, квота)
     * @param policyRegistry реестр политик прав по типам владельцев
     */
    public MediaService(
            MediaAssetRepository repository,
            AssetStorage storage,
            MediaStorageKeys storageKeys,
            MediaProperties properties,
            MediaPolicyRegistry policyRegistry) {
        this.repository = repository;
        this.storage = storage;
        this.storageKeys = storageKeys;
        this.properties = properties;
        this.policyRegistry = policyRegistry;
    }

    /**
     * Загружает картинку в слот владельца, заменяя прежнюю (если была).
     * @param ownerType тип владельца ассета
     * @param ownerId идентификатор владельца ассета
     * @param file загружаемый файл изображения
     * @param user текущий пользователь (права, квота, uploaded_by)
     * @return DTO с прокси-URL на содержимое
     */
    @Transactional
    public MediaAssetResponse upload(MediaOwnerType ownerType, UUID ownerId, MultipartFile file, MediaUser user) {
        MediaOwnerPolicy policy = policyRegistry.require(ownerType);
        policy.validateOwnerExists(ownerId);
        policy.checkUpload(ownerId, user);

        MediaLimitCategory category = ownerType.limitCategory();
        byte[] bytes = readAndValidateFile(file, category);
        ImageDimensions dimensions = readAndValidateDimensions(bytes, category);
        validateQuota(user.id(), ownerType, ownerId, bytes.length);

        UUID assetId = UUID.randomUUID();
        String contentType = file.getContentType();
        String bucket = properties.getBucket();
        String storageKey = storageKeys.objectKey(ownerType, ownerId, assetId, file.getOriginalFilename());

        storage.save(bucket, storageKey, contentType, bytes.length, new ByteArrayInputStream(bytes));

        try {
            Optional<MediaAsset> existing = repository.findByOwnerTypeAndOwnerId(ownerType, ownerId);
            existing.ifPresent(repository::delete);
            repository.flush(); // выполнить удаление до вставки — слот UNIQUE(owner_type, owner_id)

            MediaAsset entity = MediaAsset.builder()
                    .id(assetId)
                    .ownerType(ownerType)
                    .ownerId(ownerId)
                    .bucketName(bucket)
                    .storageKey(storageKey)
                    .originalFilename(storageKeys.sanitizeFilename(file.getOriginalFilename()))
                    .contentType(contentType)
                    .sizeBytes(bytes.length)
                    .widthPx(dimensions.width())
                    .heightPx(dimensions.height())
                    .checksumSha256(sha256(bytes))
                    .uploadedBy(user.id())
                    .build();
            MediaAsset saved = repository.save(entity);
            repository.flush();

            // Прежний объект удаляем только после успешного коммита новой записи, чтобы откат БД
            // не оставил слот без картинки при уже удалённом объекте.
            existing.ifPresent(old -> scheduleObjectDeletionAfterCommit(old.getBucketName(), old.getStorageKey()));
            return MediaAssetResponse.fromEntity(saved);
        } catch (RuntimeException exception) {
            // Метаданные не сохранились — удаляем свежезалитый объект, прежний слот остаётся нетронутым.
            deleteObjectQuietly(bucket, storageKey);
            throw exception;
        }
    }

    /**
     * Открывает содержимое ассета для стриминга, проверив право на чтение.
     * @param assetId идентификатор ассета
     * @param user текущий пользователь
     * @return поток содержимого с авторитетными типом и размером из записи media_asset
     */
    @Transactional(readOnly = true)
    public StoredObjectStream openContent(UUID assetId, MediaUser user) {
        MediaAsset entity = repository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Медиа-ассет не найден."));
        policyRegistry.require(entity.getOwnerType()).checkRead(entity.getOwnerId(), user);
        StoredObjectStream stream = storage.openStream(entity.getBucketName(), entity.getStorageKey());
        // Тип и размер берём из записи (источник истины), поток — из хранилища.
        return new StoredObjectStream(stream.inputStream(), entity.getContentType(), entity.getSizeBytes());
    }

    /**
     * Удаляет ассет слота: право дают загрузочная политика владельца ИЛИ роль ADMIN (модерация).
     * @param ownerType тип владельца ассета
     * @param ownerId идентификатор владельца ассета
     * @param user текущий пользователь
     */
    @Transactional
    public void delete(MediaOwnerType ownerType, UUID ownerId, MediaUser user) {
        MediaAsset entity = repository.findByOwnerTypeAndOwnerId(ownerType, ownerId)
                .orElseThrow(() -> new ResourceNotFoundException("Медиа-ассет не найден."));
        if (!user.isAdmin()) {
            policyRegistry.require(ownerType).checkUpload(ownerId, user);
        }
        repository.delete(entity);
        repository.flush();
        scheduleObjectDeletionAfterCommit(entity.getBucketName(), entity.getStorageKey());
    }

    /**
     * Читает файл и проверяет пустоту, размер и MIME-тип по allowlist.
     * @param file загружаемый файл
     * @param category категория лимитов владельца
     * @return байты файла
     */
    private byte[] readAndValidateFile(MultipartFile file, MediaLimitCategory category) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Файл изображения пуст.");
        }
        if (file.getSize() > category.maxFileSizeBytes()) {
            throw new BadRequestException("Файл слишком большой: максимум "
                    + (category.maxFileSizeBytes() / (1024 * 1024)) + " МБ.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("Неподдерживаемый тип файла. Разрешены PNG, JPEG и WEBP.");
        }
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new BadRequestException("Не удалось прочитать файл изображения.", exception);
        }
    }

    /**
     * Декодирует изображение (отсекая мусор, замаскированный под картинку) и проверяет размеры.
     * @param bytes байты файла
     * @param category категория лимитов владельца
     * @return размеры изображения в пикселях
     */
    private ImageDimensions readAndValidateDimensions(byte[] bytes, MediaLimitCategory category) {
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException exception) {
            throw new BadRequestException("Файл не является корректным изображением.", exception);
        }
        if (image == null) {
            throw new BadRequestException("Файл не является декодируемым изображением.");
        }
        int width = image.getWidth();
        int height = image.getHeight();
        if (width > category.maxWidthPx() || height > category.maxHeightPx()) {
            throw new BadRequestException("Размеры изображения превышают лимит: максимум "
                    + category.maxWidthPx() + "×" + category.maxHeightPx() + " px.");
        }
        return new ImageDimensions(width, height);
    }

    /**
     * Проверяет пользовательскую квоту с учётом освобождения байтов при замене своего же слота.
     * @param userId идентификатор пользователя
     * @param ownerType тип владельца (для учёта заменяемого слота)
     * @param ownerId идентификатор владельца (для учёта заменяемого слота)
     * @param incomingBytes размер нового файла в байтах
     */
    private void validateQuota(UUID userId, MediaOwnerType ownerType, UUID ownerId, long incomingBytes) {
        long currentTotal = repository.sumSizeBytesByUploadedBy(userId);
        long replacedBytes = repository.findByOwnerTypeAndOwnerId(ownerType, ownerId)
                .filter(existing -> userId.equals(existing.getUploadedBy()))
                .map(MediaAsset::getSizeBytes)
                .orElse(0L);
        long projectedTotal = currentTotal - replacedBytes + incomingBytes;
        if (projectedTotal > properties.getUserQuotaBytes()) {
            throw new BadRequestException("Превышена квота хранилища ("
                    + (properties.getUserQuotaBytes() / (1024 * 1024)) + " МБ). Удалите ненужные картинки.");
        }
    }

    /**
     * Вычисляет SHA-256 файла в hex для поля checksum_sha256.
     * @param bytes байты файла
     * @return контрольная сумма в шестнадцатеричном виде
     */
    private String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (Exception exception) {
            throw new IllegalStateException("Не удалось вычислить контрольную сумму изображения.", exception);
        }
    }

    /**
     * Планирует удаление объекта из хранилища после успешного коммита транзакции; вне транзакции
     * удаляет сразу.
     * @param bucket бакет объекта
     * @param storageKey ключ объекта
     */
    private void scheduleObjectDeletionAfterCommit(String bucket, String storageKey) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteObjectQuietly(bucket, storageKey);
                }
            });
        } else {
            deleteObjectQuietly(bucket, storageKey);
        }
    }

    /**
     * Удаляет объект «по-тихому»: ошибка хранилища логируется, но не роняет операцию.
     * @param bucket бакет объекта
     * @param storageKey ключ объекта
     */
    private void deleteObjectQuietly(String bucket, String storageKey) {
        try {
            storage.delete(bucket, storageKey);
        } catch (RuntimeException exception) {
            log.warn("MediaService: не удалось удалить объект хранилища bucket={}, key={}",
                    bucket, storageKey, exception);
        }
    }

    /**
     * Внутренняя пара размеров изображения.
     * @param width ширина в пикселях
     * @param height высота в пикселях
     */
    private record ImageDimensions(int width, int height) {
    }
}
