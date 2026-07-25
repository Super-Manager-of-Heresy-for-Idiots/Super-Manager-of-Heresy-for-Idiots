package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс AttachLocationMapRequest описывает DTO запроса привязки карты map-service
 * к локации кампании (WORLD_PLAN Этап 4).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachLocationMapRequest {

    @NotNull(message = "externalMapId is required")
    private UUID externalMapId;

    /** Сделать карту картой локации по умолчанию. */
    private Boolean isDefault;
}
