package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс LocationResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocationResponse {
    private UUID id;
    private String name;
    private String description;
    private Boolean isVisibleToPlayers;
    /** Превью локации: прокси-URL media-ассета или null, если не загружено. */
    private String previewUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
