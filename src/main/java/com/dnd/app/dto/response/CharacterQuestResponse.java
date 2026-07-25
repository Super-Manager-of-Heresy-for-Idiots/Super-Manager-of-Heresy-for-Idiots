package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс CharacterQuestResponse описывает запись журнала квестов персонажа
 * (WORLD_PLAN Этап 2): персональный статус + краткие данные самого квеста.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CharacterQuestResponse {
    private UUID id;
    private UUID questId;
    private String title;
    private String description;
    /** Мастер-статус квеста в кампании (ACTIVE/COMPLETED/...). */
    private String questStatus;
    /** Персональный статус в журнале (ACCEPTED/COMPLETED/FAILED/ABANDONED). */
    private String status;
    private UUID givenByNpcId;
    private String givenByNpcName;
    private Instant acceptedAt;
    private Instant completedAt;
}
