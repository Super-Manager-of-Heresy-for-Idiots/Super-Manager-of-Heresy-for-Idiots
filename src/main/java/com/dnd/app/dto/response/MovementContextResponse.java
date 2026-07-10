package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс MovementContextResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovementContextResponse {

    /** The combatant whose turn it is, or null if the battle is not active. */
    private UUID activeCombatantId;

    private int roundNumber;

    private List<CombatantMovement> combatants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CombatantMovement {
        private UUID combatantId;
        private int speedFt;
        private int movementUsedFt;
        private int remainingFt;
    }
}
