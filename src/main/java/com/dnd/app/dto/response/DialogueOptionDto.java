package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс DialogueOptionDto описывает вариант ответа игрока в узле диалога NPC (WORLD_PLAN Этап 4).
 * Либо ведёт к следующему узлу ({@code nextNodeId}), либо выполняет действие ({@code actionType}:
 * END/OPEN_SHOP/OFFER_QUEST). Используется и в хранимом JSON-дереве, и в API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DialogueOptionDto {
    private String text;
    private String nextNodeId;
    private String actionType;
    private UUID questId;
}
