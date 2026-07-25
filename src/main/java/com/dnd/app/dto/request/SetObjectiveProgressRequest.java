package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс SetObjectiveProgressRequest описывает запрос мастера на выставление прогресса персонажа
 * по цели квеста (WORLD_PLAN Этап 3). Значение задаётся абсолютно (текущий счётчик).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetObjectiveProgressRequest {

    @NotNull(message = "currentCount is required")
    @PositiveOrZero(message = "currentCount must be zero or positive")
    private Integer currentCount;
}
