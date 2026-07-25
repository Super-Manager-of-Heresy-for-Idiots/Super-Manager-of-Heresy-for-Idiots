package com.dnd.app.domain.enums;

/**
 * Перечисление WebSocketEventType описывает перечисление домена, которое фиксирует допустимые значения игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public enum WebSocketEventType {
    ITEM_GRANTED,
    ITEM_REMOVED,
    BUFF_APPLIED,
    BUFF_REMOVED,
    XP_GRANTED,
    HP_CHANGED,
    CHARACTER_UPDATED,
    NPC_REVEALED,
    NPC_HIDDEN,
    LOCATION_REVEALED,
    LOCATION_HIDDEN,
    MONSTER_REVEALED,
    MONSTER_HIDDEN,
    QUEST_UPDATED,
    CAMPAIGN_STATUS_CHANGED,
    MEMBER_KICKED,
    WALLET_CHANGED,
    BATTLE_STARTED,
    BATTLE_UPDATED,
    BATTLE_ENDED,
    COMBATANT_JOINED,
    BATTLE_TURN_CHANGED,
    BATTLE_ACTION,
    BATTLE_LOG_APPENDED,
    COMBATANT_CONDITIONS_CHANGED,
    FRIEND_REQUEST_RECEIVED,
    FRIEND_REQUEST_ACCEPTED,
    FRIEND_REMOVED,
    // WORLD_PLAN: присутствие в мире, журнал квестов, переходы между картами.
    LOCATION_PRESENCE_CHANGED,
    QUEST_ACCEPTED,
    QUEST_ABANDONED,
    QUEST_TURNED_IN,
    SHOP_UPDATED,
    MAP_TRANSITION_TRAVERSED,
    // ROLL_PROMPT: мастер запрашивает проверку — окно броска у игрока, результат мастеру.
    ROLL_PROMPT_CREATED,
    ROLL_PROMPT_RESOLVED,
    ROLL_PROMPT_CANCELLED
}
