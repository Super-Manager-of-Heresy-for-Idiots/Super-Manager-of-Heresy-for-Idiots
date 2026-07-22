package com.dnd.app.dto.request;

import com.dnd.app.dto.MapCellDto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс CreateMapTransitionRequest описывает DTO запроса создания перехода между картами
 * (WORLD_PLAN Этап 5: ключевые клетки).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMapTransitionRequest {

    @NotNull(message = "fromMapId is required")
    private UUID fromMapId;

    @NotEmpty(message = "fromCells must contain at least one cell")
    private List<MapCellDto> fromCells;

    @NotNull(message = "toMapId is required")
    private UUID toMapId;

    @NotNull(message = "toCell is required")
    private MapCellDto toCell;

    /** Локация мира, в которую попадает персонаж (опционально). */
    private UUID toLocationId;

    @Size(max = 120, message = "Label must be at most 120 characters")
    private String label;

    private Boolean enabled;
}
