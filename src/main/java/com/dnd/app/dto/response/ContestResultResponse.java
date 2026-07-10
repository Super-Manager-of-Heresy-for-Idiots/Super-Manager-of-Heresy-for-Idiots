package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс ContestResultResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContestResultResponse {

    private String type;
    private String attackerName;
    private String targetName;
    private int attackerRoll;
    private int attackerTotal;
    private int targetRoll;
    private int targetTotal;
    private boolean attackerWins;
    /** Condition applied to the target on a win ("grappled" | "prone"); null on a loss or a PUSH shove. */
    private String condition;

    private BattleResponse battle;
}
