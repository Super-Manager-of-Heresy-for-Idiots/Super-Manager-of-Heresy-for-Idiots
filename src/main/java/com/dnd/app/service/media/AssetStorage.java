package com.dnd.app.service.media;

import java.io.InputStream;

/**
 * Интерфейс AssetStorage абстрагирует объектное хранилище (MinIO) от бизнес-логики media-модуля.
 * Реализация поднимается только когда настроен MinIO; сервис работает через этот контракт и не
 * зависит напрямую от SDK. Метод {@code copy} нужен для форка blueprint (Фаза 4): копия объекта
 * без повторной загрузки файла клиентом.
 */
public interface AssetStorage {

    /**
     * Сохраняет объект в хранилище (создаёт бакет при первом обращении).
     * @param bucketName имя бакета назначения
     * @param storageKey ключ (путь) объекта в бакете
     * @param contentType MIME-тип сохраняемого объекта
     * @param sizeBytes точный размер потока в байтах
     * @param inputStream поток содержимого объекта
     */
    void save(String bucketName, String storageKey, String contentType, long sizeBytes, InputStream inputStream);

    /**
     * Открывает поток содержимого объекта вместе с его метаданными.
     * @param bucketName имя бакета, где лежит объект
     * @param storageKey ключ (путь) объекта в бакете
     * @return поток и метаданные объекта; бросает 404, если объект не найден
     */
    StoredObjectStream openStream(String bucketName, String storageKey);

    /**
     * Удаляет объект из хранилища.
     * @param bucketName имя бакета, где лежит объект
     * @param storageKey ключ (путь) удаляемого объекта
     */
    void delete(String bucketName, String storageKey);

    /**
     * Копирует объект внутри хранилища (создаёт бакет назначения при необходимости).
     * @param sourceBucket бакет-источник
     * @param sourceKey ключ объекта-источника
     * @param targetBucket бакет назначения
     * @param targetKey ключ объекта назначения
     */
    void copy(String sourceBucket, String sourceKey, String targetBucket, String targetKey);
}
