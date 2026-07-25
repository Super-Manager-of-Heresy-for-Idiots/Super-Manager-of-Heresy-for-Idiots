package com.dnd.app.util;

import com.dnd.app.domain.enums.CampStatus;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Класс CampStatusRules описывает state-machine привала: единственный источник правды
 * о допустимых переходах статуса. Интерфейс строит кнопки мастера из этого же списка,
 * поэтому невозможных действий на экране не появляется.
 */
public final class CampStatusRules {

    private static final Map<CampStatus, List<CampStatus>> TRANSITIONS = new EnumMap<>(CampStatus.class);

    static {
        TRANSITIONS.put(CampStatus.SETTING_UP, List.of(CampStatus.ACTIVE, CampStatus.COMPLETED));
        TRANSITIONS.put(CampStatus.ACTIVE,
                List.of(CampStatus.RESTING, CampStatus.INTERRUPTED, CampStatus.COMPLETED));
        TRANSITIONS.put(CampStatus.RESTING, List.of(CampStatus.INTERRUPTED, CampStatus.COMPLETED));
        TRANSITIONS.put(CampStatus.INTERRUPTED, List.of(CampStatus.ACTIVE, CampStatus.COMPLETED));
        TRANSITIONS.put(CampStatus.COMPLETED, List.of());
    }

    private CampStatusRules() {}

    /**
     * Возвращает статусы, в которые привал может перейти из текущего.
     * @param from текущий статус привала
     * @return допустимые целевые статусы
     */
    public static List<CampStatus> allowedTransitions(CampStatus from) {
        return TRANSITIONS.getOrDefault(from, List.of());
    }

    /**
     * Проверяет допустимость перехода привала между статусами.
     * @param from текущий статус привала
     * @param to целевой статус привала
     * @return true, если переход разрешён state-machine
     */
    public static boolean canTransition(CampStatus from, CampStatus to) {
        return allowedTransitions(from).contains(to);
    }
}
