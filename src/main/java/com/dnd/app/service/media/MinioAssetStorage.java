package com.dnd.app.service.media;

import com.dnd.app.exception.ResourceNotFoundException;
import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Класс MinioAssetStorage — реализация {@link AssetStorage} поверх клиента MinIO.
 * Поднимается только при заданном {@code minio.endpoint}, поэтому без MinIO бин отсутствует и
 * media-модуль выключен. Бакет создаётся лениво при первом сохранении/копировании
 * ({@code ensureBucket}). Ошибки хранилища оборачиваются в {@link IllegalStateException} (→ 500),
 * а отсутствие объекта при чтении — в {@link ResourceNotFoundException} (→ 404).
 */
@Component
@ConditionalOnProperty(name = "minio.endpoint")
public class MinioAssetStorage implements AssetStorage {

    private final MinioClient minioClient;

    /**
     * Создаёт хранилище поверх клиента MinIO.
     * @param minioClient клиент MinIO из {@link com.dnd.app.config.MinioConfig}
     */
    public MinioAssetStorage(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Сохраняет объект в бакете (создаёт бакет при первом обращении).
     * @param bucketName имя бакета назначения
     * @param storageKey ключ объекта
     * @param contentType MIME-тип объекта
     * @param sizeBytes размер потока в байтах
     * @param inputStream поток содержимого
     */
    @Override
    public void save(String bucketName, String storageKey, String contentType, long sizeBytes, InputStream inputStream) {
        try {
            ensureBucket(bucketName);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storageKey)
                    .contentType(contentType)
                    .stream(inputStream, sizeBytes, -1)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Object storage is unavailable while saving media asset.", exception);
        }
    }

    /**
     * Открывает поток содержимого объекта, читая тип и размер из заголовков ответа MinIO.
     * @param bucketName имя бакета
     * @param storageKey ключ объекта
     * @return поток и метаданные объекта
     */
    @Override
    public StoredObjectStream openStream(String bucketName, String storageKey) {
        try {
            GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storageKey)
                    .build());
            String contentType = response.headers().get("Content-Type");
            String contentLength = response.headers().get("Content-Length");
            Long sizeBytes = contentLength != null ? parseLongOrNull(contentLength) : null;
            return new StoredObjectStream(response, contentType, sizeBytes);
        } catch (Exception exception) {
            throw new ResourceNotFoundException("Содержимое медиа-ассета не найдено в объектном хранилище.");
        }
    }

    /**
     * Удаляет объект из бакета.
     * @param bucketName имя бакета
     * @param storageKey ключ удаляемого объекта
     */
    @Override
    public void delete(String bucketName, String storageKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(storageKey)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Object storage is unavailable while deleting media asset.", exception);
        }
    }

    /**
     * Копирует объект внутри хранилища (для форка blueprint).
     * @param sourceBucket бакет-источник
     * @param sourceKey ключ источника
     * @param targetBucket бакет назначения
     * @param targetKey ключ назначения
     */
    @Override
    public void copy(String sourceBucket, String sourceKey, String targetBucket, String targetKey) {
        try {
            ensureBucket(targetBucket);
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(targetBucket)
                    .object(targetKey)
                    .source(CopySource.builder()
                            .bucket(sourceBucket)
                            .object(sourceKey)
                            .build())
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Object storage is unavailable while copying media asset.", exception);
        }
    }

    /**
     * Создаёт бакет, если его ещё нет.
     * @param bucketName имя проверяемого/создаваемого бакета
     * @throws Exception если обращение к MinIO не удалось
     */
    private void ensureBucket(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    /**
     * Безопасно парсит длину контента; возвращает null при некорректном значении.
     * @param value строковое значение заголовка Content-Length
     * @return размер в байтах или null
     */
    private Long parseLongOrNull(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
