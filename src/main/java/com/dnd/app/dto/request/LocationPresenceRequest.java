package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс LocationPresenceRequest описывает DTO запроса входа/выхода персонажа из локации
 * (WORLD_PLAN Этап 1: присутствие в мире).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationPresenceRequest {

    @NotNull(message = "characterId is required")
    private UUID characterId;
}
