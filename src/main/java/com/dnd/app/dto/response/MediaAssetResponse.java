package com.dnd.app.dto.response;

import com.dnd.app.domain.MediaAsset;

import java.util.UUID;

/**
 * Record MediaAssetResponse — ответ API после загрузки картинки. Поле {@code url} — это относительный
 * прокси-путь на стрим содержимого ({@code /api/media/{assetId}/content}); наружу presigned-ссылки
 * не отдаются (проблема внутреннего DNS MinIO). Клиент использует {@code url} прямо в {@code <img src>}.
 *
 * @param assetId идентификатор загруженного ассета
 * @param url относительный путь на стрим содержимого
 * @param contentType MIME-тип изображения
 * @param sizeBytes размер файла в байтах
 * @param widthPx ширина изображения в пикселях
 * @param heightPx высота изображения в пикселях
 */
public record MediaAssetResponse(
        UUID assetId,
        String url,
        String contentType,
        long sizeBytes,
        Integer widthPx,
        Integer heightPx
) {

    /**
     * Строит ответ из сохранённой сущности ассета.
     * @param entity сохранённая запись media_asset
     * @return DTO ответа с прокси-URL на содержимое
     */
    public static MediaAssetResponse fromEntity(MediaAsset entity) {
        return new MediaAssetResponse(
                entity.getId(),
                "/api/media/" + entity.getId() + "/content",
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getWidthPx(),
                entity.getHeightPx()
        );
    }
}
