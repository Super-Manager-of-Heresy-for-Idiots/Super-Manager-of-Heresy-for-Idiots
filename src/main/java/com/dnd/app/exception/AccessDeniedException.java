package com.dnd.app.exception;

/**
 * Класс AccessDeniedException описывает исключение бизнес-логики, которое сообщает о нарушении ожидаемого сценария.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public class AccessDeniedException extends RuntimeException {
    /**
     * Создает экземпляр компонента приложения и получает зависимости, необходимые для выполнения бизнес-логики.
     * @param message входящее значение message, используемое бизнес-сценарием
     */
    public AccessDeniedException(String message) {
        super(message);
    }
}
