package com.dnd.app.domain.enums;

/**
 * Перечисление CampEventType описывает тип записи в журнале привала.
 * Механических автоэффектов у событий нет — система фиксирует факт, награды выдаёт мастер;
 * исключение — AMBUSH, из которой мастер может создать бой.
 */
public enum CampEventType {
    AMBUSH,
    ENCOUNTER,
    STORY,
    WEATHER,
    CUSTOM
}
