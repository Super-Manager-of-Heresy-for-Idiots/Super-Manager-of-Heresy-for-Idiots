package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс LocationMapResponse описывает привязку карты map-service к локации кампании
 * (WORLD_PLAN Этап 4).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationMapResponse {
    private UUID id;
    private UUID locationId;
    private UUID externalMapId;
    private Boolean isDefault;
    private Instant createdAt;
}
