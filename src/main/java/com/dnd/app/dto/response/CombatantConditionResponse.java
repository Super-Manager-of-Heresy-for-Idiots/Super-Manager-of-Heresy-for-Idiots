package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс CombatantConditionResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombatantConditionResponse {
    private UUID conditionId;
    private String code;
    private String name;
    private String sourceText;
    /** Rounds left; null = until removed. */
    private Integer remainingRounds;
}
