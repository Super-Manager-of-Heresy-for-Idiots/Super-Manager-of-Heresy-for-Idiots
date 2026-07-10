package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс CombatantReferenceResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombatantReferenceResponse {

    private UUID battleId;
    private UUID campaignId;
    private UUID combatantId;
    private String type;
    private String displayName;
    private UUID characterId;
    private UUID monsterId;
    private UUID ownerUserId;
    private Integer currentHp;
    private Integer maxHp;
    private Integer turnOrder;
    private boolean currentTurn;
    private int widthCells;
    private int heightCells;
}
