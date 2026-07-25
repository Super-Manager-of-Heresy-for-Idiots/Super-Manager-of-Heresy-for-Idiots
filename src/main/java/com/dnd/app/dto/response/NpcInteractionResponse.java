package com.dnd.app.dto.response;

import com.dnd.app.domain.enums.NpcRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс NpcInteractionResponse описывает агрегированный ответ "подойти к NPC"
 * (WORLD_PLAN Этап 3): публичное описание, доступные квесты (если квестодатель)
 * и витрина товаров (если торговец). Приватные данные ГМа сюда не попадают.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NpcInteractionResponse {

    private UUID npcId;
    private String name;
    private String publicDescription;
    private NpcRole npcRole;
    private String portraitUrl;
    private LocationRefResponse location;

    /** Квесты, которые персонаж может взять у NPC (пусто, если нет). */
    private List<QuestResponse> availableQuests;

    /** Витрина торговца; null, если NPC не торговец. */
    private List<ShopItemResponse> shopItems;
}
