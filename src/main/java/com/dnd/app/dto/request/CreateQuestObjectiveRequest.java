package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс CreateQuestObjectiveRequest описывает запрос добавления опциональной цели квеста
 * (WORLD_PLAN Этап 3, только мастер). Тип обязателен; ссылка/подпись/счётчик — по необходимости.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuestObjectiveRequest {

    @NotBlank(message = "objectiveType is required")
    private String objectiveType;

    /** Ссылка на связанную сущность (item_template / бестиарий / npc / локация), если применимо. */
    private UUID targetRef;

    @Size(max = 200, message = "targetLabel must not exceed 200 characters")
    private String targetLabel;

    /** Требуемое количество; по умолчанию 1. */
    private Integer requiredCount;

    /** Порядок отображения; по умолчанию 0. */
    private Integer orderIndex;
}
