package com.dnd.app.dto.response;

import com.dnd.app.dto.MapCellDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Класс MapTransitionResponse описывает переход между картами (WORLD_PLAN Этап 5)
 * для отрисовки ключевых клеток на карте и редактора ГМ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MapTransitionResponse {
    private UUID id;
    private UUID campaignId;
    private UUID fromMapId;
    private List<MapCellDto> fromCells;
    private UUID toMapId;
    private MapCellDto toCell;
    private LocationRefResponse toLocation;
    private String label;
    private Boolean enabled;
    private Instant createdAt;
    private Instant updatedAt;
}
