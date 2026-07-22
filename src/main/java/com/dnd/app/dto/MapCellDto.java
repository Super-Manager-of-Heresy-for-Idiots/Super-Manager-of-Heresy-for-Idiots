package com.dnd.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс MapCellDto описывает клетку сетки карты map-service (grid-координаты),
 * используется переходами между картами (WORLD_PLAN Этап 5).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MapCellDto {
    private Integer gridX;
    private Integer gridY;
}
