package com.dnd.app.util;

/**
 * Класс AbilityScores описывает утилиту, которая поддерживает повторяемые операции бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public final class AbilityScores {

    private AbilityScores() {}

    /**
     * Выполняет операции "modifier" в рамках бизнес-логики приложения.
     * @param score входящее значение score, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static int modifier(int score) {
        return Math.floorDiv(score - 10, 2);
    }
}
