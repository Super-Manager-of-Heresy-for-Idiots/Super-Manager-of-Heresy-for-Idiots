package com.dnd.app.service.media;

import java.io.InputStream;

/**
 * Record StoredObjectStream переносит открытый поток объекта из хранилища вместе с его
 * метаданными для стриминга клиенту. {@code contentType} и {@code sizeBytes} могут быть
 * {@code null}, если хранилище не вернуло соответствующих заголовков — тогда вызывающий код
 * берёт значения из записи {@code media_asset}.
 *
 * @param inputStream открытый поток содержимого объекта (закрывает вызывающий)
 * @param contentType MIME-тип объекта из хранилища, либо null
 * @param sizeBytes размер объекта в байтах из хранилища, либо null
 */
public record StoredObjectStream(InputStream inputStream, String contentType, Long sizeBytes) {
}
