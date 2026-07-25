package com.dnd.app.dto.response;

import com.dnd.app.dto.MapCellDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс TraverseResultResponse описывает результат прохода через переход между картами
 * (WORLD_PLAN Этап 5): куда попал персонаж и был ли перенесён его токен.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TraverseResultResponse {
    private UUID transitionId;
    private UUID characterId;
    private UUID toMapId;
    private MapCellDto toCell;
    private LocationRefResponse toLocation;
    /** Перенесён ли токен на целевую карту силами map-service. */
    private Boolean tokenMoved;
    /** Покинул ли персонаж активный бой при переходе. */
    private Boolean leftBattle;
}
