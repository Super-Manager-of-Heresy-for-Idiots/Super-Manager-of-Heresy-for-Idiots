package com.dnd.app.service.formula;

/**
 * Запись DiceValue описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 * @param count входящее значение count, используемое бизнес-сценарием
 * @param sides входящее значение sides, используемое бизнес-сценарием
 */
public record DiceValue(int count, int sides) {

    /**
     * Преобразует данные операции "to expression" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    public String toExpression() {
        return count + "d" + sides;
    }

    /**
     * Выполняет операции "average" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    public double average() {
        return count * (sides + 1) / 2.0;
    }
}
