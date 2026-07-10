package com.dnd.app.exception;

/**
 * Класс PreconditionFailedException описывает исключение бизнес-логики, которое сообщает о нарушении ожидаемого сценария.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public class PreconditionFailedException extends RuntimeException {
    /**
     * Создает экземпляр компонента приложения и получает зависимости, необходимые для выполнения бизнес-логики.
     * @param message входящее значение message, используемое бизнес-сценарием
     */
    public PreconditionFailedException(String message) {
        super(message);
    }
}
