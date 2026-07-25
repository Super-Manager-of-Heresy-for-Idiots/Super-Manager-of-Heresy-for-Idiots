package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс ObjectiveProgressResponse описывает прогресс персонажа по цели квеста
 * (WORLD_PLAN Этап 3): определение цели + текущее/требуемое значение и признак выполнения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ObjectiveProgressResponse {
    private UUID objectiveId;
    private String objectiveType;
    private String targetLabel;
    private Integer requiredCount;
    private Integer currentCount;
    private Boolean completed;
}
