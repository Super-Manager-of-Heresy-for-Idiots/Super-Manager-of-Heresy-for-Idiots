package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс BattleLogEntryResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleLogEntryResponse {
    private UUID id;
    private long seq;
    private String type;
    private UUID actorCombatantId;
    private UUID targetCombatantId;
    /** Parsed JSON payload (may be null). */
    private Object payload;
    private String visibility;
    private Instant createdAt;
}
