package com.dnd.app.dto.request;

import com.dnd.app.dto.MapCellDto;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс UpdateMapTransitionRequest описывает DTO запроса обновления перехода между картами
 * (WORLD_PLAN Этап 5). Все поля опциональны; null — не менять.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMapTransitionRequest {

    private List<MapCellDto> fromCells;

    private MapCellDto toCell;

    private UUID toLocationId;

    /** true — снять локацию с перехода (toLocationId игнорируется). */
    private Boolean clearToLocation;

    @Size(max = 120, message = "Label must be at most 120 characters")
    private String label;

    private Boolean enabled;
}
