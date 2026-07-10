package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Класс ApiResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String message;
    private String error;
    private Map<String, String> fields;

    /**
     * Выполняет операции "ok" в рамках бизнес-логики ответов API.
     * @param data входящее значение data, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }

    /**
     * Выполняет операции "ok" в рамках бизнес-логики ответов API.
     * @param data входящее значение data, используемое бизнес-сценарием
     * @param message входящее значение message, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder().success(true).data(data).message(message).build();
    }

    /**
     * Выполняет операции "error" в рамках бизнес-логики ответов API.
     * @param error входящее значение error, используемое бизнес-сценарием
     * @param message входящее значение message, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static <T> ApiResponse<T> error(String error, String message) {
        return ApiResponse.<T>builder().success(false).error(error).message(message).build();
    }

    /**
     * Выполняет операции "validation error" в рамках бизнес-логики ответов API.
     * @param message входящее значение message, используемое бизнес-сценарием
     * @param fields входящее значение fields, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static <T> ApiResponse<T> validationError(String message, Map<String, String> fields) {
        return ApiResponse.<T>builder()
                .success(false)
                .error("VALIDATION_ERROR")
                .message(message)
                .fields(fields)
                .build();
    }
}
