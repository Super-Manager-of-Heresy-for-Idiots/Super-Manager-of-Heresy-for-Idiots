package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс QuestObjectiveResponse описывает опциональную цель квеста в ответе API
 * (WORLD_PLAN Этап 3): определение цели без персонального прогресса.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestObjectiveResponse {
    private UUID id;
    private String objectiveType;
    private UUID targetRef;
    private String targetLabel;
    private Integer requiredCount;
    private Integer orderIndex;
}
