package com.dnd.app.domain.enums;

/**
 * Перечисление CampInterruptReason описывает причину перевода привала в статус INTERRUPTED:
 * засада с созданным боем, прочее событие журнала или ручное решение мастера.
 */
public enum CampInterruptReason {
    AMBUSH,
    EVENT,
    MANUAL
}
