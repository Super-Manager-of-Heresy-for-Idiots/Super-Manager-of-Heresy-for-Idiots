package com.dnd.app.exception;

/**
 * Класс BadRequestException описывает исключение бизнес-логики, которое сообщает о нарушении ожидаемого сценария.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public class BadRequestException extends RuntimeException {
    /**
     * Создает экземпляр компонента приложения и получает зависимости, необходимые для выполнения бизнес-логики.
     * @param message входящее значение message, используемое бизнес-сценарием
     */
    public BadRequestException(String message) {
        super(message);
    }

    /**
     * Создает экземпляр компонента приложения и получает зависимости, необходимые для выполнения бизнес-логики.
     * @param message входящее значение message, используемое бизнес-сценарием
     * @param cause входящее значение cause, используемое бизнес-сценарием
     */
    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
