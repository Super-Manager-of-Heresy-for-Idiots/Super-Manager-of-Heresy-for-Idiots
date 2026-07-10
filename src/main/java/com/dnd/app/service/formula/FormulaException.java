package com.dnd.app.service.formula;

/** Raised for any parse or evaluation problem in the formula DSL (unknown variable/function, type
 *  mismatch, division by zero, syntax error). Carried up as a controlled validation failure. */
/**
 * Класс FormulaException описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public class FormulaException extends RuntimeException {
    /**
     * Создает экземпляр компонента домена и получает зависимости, необходимые для выполнения бизнес-логики.
     * @param message входящее значение message, используемое бизнес-сценарием
     */
    public FormulaException(String message) {
        super(message);
    }
}
