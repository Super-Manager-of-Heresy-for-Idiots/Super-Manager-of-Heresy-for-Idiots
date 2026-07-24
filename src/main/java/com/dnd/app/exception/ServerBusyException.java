package com.dnd.app.exception;

/**
 * Класс ServerBusyException описывает исключение перегрузки: пул «запросов в полёте» исчерпан и
 * запрос не удалось принять за отведённое время ожидания (N9 admission control). Бросается синхронно
 * в потоке контроллера (до возврата CompletableFuture), поэтому его штатно ловит GlobalExceptionHandler
 * и отвечает 503 + Retry-After. Это осознанная быстрая деградация, а не изменение бизнес-логики.
 */
public class ServerBusyException extends RuntimeException {
    /**
     * Создаёт исключение перегрузки.
     * @param message входящее значение message, используемое бизнес-сценарием
     */
    public ServerBusyException(String message) {
        super(message);
    }
}
