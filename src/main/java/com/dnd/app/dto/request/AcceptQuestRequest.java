package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс AcceptQuestRequest описывает DTO запроса взятия квеста у NPC персонажем
 * (WORLD_PLAN Этап 2: журнал квестов).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcceptQuestRequest {

    @NotNull(message = "characterId is required")
    private UUID characterId;
}
