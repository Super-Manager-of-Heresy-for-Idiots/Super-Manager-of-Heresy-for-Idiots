package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс ValidationIssue описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ValidationIssue", description = "A structured validation issue keyed by request path")
public class ValidationIssue {

    @Schema(description = "Path into the request",
            example = "rewardGroups[2].options[0].grants[1].abilityOptionIds")
    private String path;

    @Schema(description = "Machine code", example = "INVALID_REFERENCE")
    private String code;

    @Schema(description = "ERROR | WARNING", example = "ERROR")
    private String severity;

    @Schema(description = "Human-readable message")
    private String message;

    /**
     * Выполняет операции "error" в рамках бизнес-логики передачи данных.
     * @param path входящее значение path, используемое бизнес-сценарием
     * @param code входящее значение code, используемое бизнес-сценарием
     * @param message входящее значение message, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static ValidationIssue error(String path, String code, String message) {
        return ValidationIssue.builder().path(path).code(code).severity("ERROR").message(message).build();
    }

    /**
     * Выполняет операции "warning" в рамках бизнес-логики передачи данных.
     * @param path входящее значение path, используемое бизнес-сценарием
     * @param code входящее значение code, используемое бизнес-сценарием
     * @param message входящее значение message, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static ValidationIssue warning(String path, String code, String message) {
        return ValidationIssue.builder().path(path).code(code).severity("WARNING").message(message).build();
    }

    /**
     * Проверяет условие операции "is error" в рамках бизнес-логики передачи данных.
     * @return результат выполнения бизнес-операции
     */
    public boolean isError() {
        return "ERROR".equals(severity);
    }
}
