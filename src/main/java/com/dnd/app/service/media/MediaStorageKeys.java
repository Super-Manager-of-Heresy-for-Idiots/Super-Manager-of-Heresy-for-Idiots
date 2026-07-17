package com.dnd.app.service.media;

import com.dnd.app.domain.enums.MediaOwnerType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.UUID;

/**
 * Класс MediaStorageKeys строит ключи объектов в бакете по схеме
 * {@code media/{owner_type}/{owner_id}/{asset_id}/{sanitized-filename}} и санитизирует имена
 * файлов. Санитизация повторяет подход map-service: убирает путь, приводит к нижнему регистру и
 * заменяет всё, кроме {@code [a-z0-9._-]}, на дефис — чтобы имя было безопасным ключом в S3/MinIO.
 */
@Component
public class MediaStorageKeys {

    /**
     * Строит ключ объекта для ассета конкретного владельца.
     * @param ownerType тип владельца (первый сегмент пути)
     * @param ownerId идентификатор владельца
     * @param assetId идентификатор ассета (гарантирует уникальность пути даже при том же имени файла)
     * @param originalFilename исходное имя файла от клиента
     * @return ключ объекта в бакете
     */
    public String objectKey(MediaOwnerType ownerType, UUID ownerId, UUID assetId, String originalFilename) {
        return "media/%s/%s/%s/%s".formatted(
                ownerType.name().toLowerCase(Locale.ROOT),
                ownerId,
                assetId,
                sanitizeFilename(originalFilename));
    }

    /**
     * Приводит имя файла к безопасному виду для использования в ключе объекта.
     * @param originalFilename исходное имя файла (может быть null/пустым)
     * @return санитизированное имя файла (никогда не пустое)
     */
    public String sanitizeFilename(String originalFilename) {
        String filename = StringUtils.hasText(originalFilename) ? originalFilename : "image";
        filename = filename.replace('\\', '/');
        int lastSlash = filename.lastIndexOf('/');
        if (lastSlash >= 0) {
            filename = filename.substring(lastSlash + 1);
        }
        filename = filename.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-");
        filename = filename.replaceAll("-+", "-").replaceAll("^\\.+", "");
        return StringUtils.hasText(filename) ? filename : "image";
    }
}
