package com.dnd.app.dto.response;

import com.dnd.app.domain.enums.NpcRole;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
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

    /** Взятые квесты, которые персонаж может сдать этому NPC (ACCEPTED/READY_FOR_TURN_IN). */
    private List<CharacterQuestResponse> turnInQuests;

    /** Витрина торговца; null, если NPC не торговец. */
    private List<ShopItemResponse> shopItems;

    /** Опциональный диалог NPC (WORLD_PLAN Этап 4); null, если мастер его не настроил. */
    private NpcDialogueResponse dialogue;

    /**
     * Курс выкупа товаров торговцем в процентах от базовой цены (для показа цены продажи).
     * null, если NPC не торговец. Значение приходит с бэкенда, чтобы клиент не дублировал константу.
     */
    private Integer buybackRatePercent;

    /**
     * Баланс золота взаимодействующего персонажа — чтобы игрок видел, хватает ли средств на покупку.
     * null, если персонаж не задан (например, ГМ смотрит без персонажа) или у него нет кошелька золота.
     */
    private BigDecimal goldBalance;
}
