package com.dnd.app.domain.enums;

/**
 * Перечисление CampStatus описывает состояние привала: разбивка лагеря, стоянка,
 * идущий отдых, прерывание и завершение. Управляет допустимыми действиями мастера.
 */
public enum CampStatus {
    SETTING_UP,
    ACTIVE,
    RESTING,
    INTERRUPTED,
    COMPLETED
}
