package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс TraverseTransitionRequest описывает DTO запроса прохода персонажа через переход
 * между картами (WORLD_PLAN Этап 5). tokenId/fromSessionId опциональны: без них
 * выполняется только смена локации мира (без переноса токена на доске).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraverseTransitionRequest {

    @NotNull(message = "characterId is required")
    private UUID characterId;

    /** Токен персонажа на исходной карте (для валидации позиции и переноса). */
    private UUID tokenId;

    /** Активная сессия исходной карты. */
    private UUID fromSessionId;
}
