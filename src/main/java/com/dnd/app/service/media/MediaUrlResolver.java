package com.dnd.app.service.media;

import com.dnd.app.domain.enums.MediaOwnerType;
import com.dnd.app.repository.MediaAssetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс MediaUrlResolver — единая точка выбора URL картинки для DTO. Если для владельца есть
 * media-ассет, возвращает прокси-путь {@code /api/media/{assetId}/content}; иначе отдаёт легаси-URL
 * (например, внешний {@code characters.avatar_url}), чтобы старые данные продолжали работать (fallback).
 * Зависит только от таблицы {@code media_asset} (не от MinIO-бинов), поэтому работает и при выключенном
 * модуле: тогда ассетов нет и всегда возвращается легаси-URL. Общий хелпер — чтобы логика fallback не
 * копировалась по мапперам.
 */
@Component
@RequiredArgsConstructor
public class MediaUrlResolver {

    private final MediaAssetRepository repository;

    /**
     * Возвращает URL картинки владельца: media-ассет приоритетнее легаси-URL.
     * @param ownerType тип владельца ассета
     * @param ownerId идентификатор владельца (если null — сразу возвращается легаси-URL)
     * @param legacyUrl запасной URL из старой колонки (может быть null)
     * @return прокси-URL media-ассета, если он есть, иначе legacyUrl
     */
    public String resolve(MediaOwnerType ownerType, UUID ownerId, String legacyUrl) {
        if (ownerId == null) {
            return legacyUrl;
        }
        return repository.findByOwnerTypeAndOwnerId(ownerType, ownerId)
                .map(asset -> "/api/media/" + asset.getId() + "/content")
                .orElse(legacyUrl);
    }
}
