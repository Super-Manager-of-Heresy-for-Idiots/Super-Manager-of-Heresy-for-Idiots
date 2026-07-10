package com.dnd.app.exception;

import com.dnd.app.dto.content.ValidationIssue;
import lombok.Getter;

import java.util.List;

/**
 * Класс ClassValidationException описывает исключение бизнес-логики, которое сообщает о нарушении ожидаемого сценария.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Getter
public class ClassValidationException extends RuntimeException {

    private final transient List<ValidationIssue> issues;

    /**
     * Создает экземпляр компонента приложения и получает зависимости, необходимые для выполнения бизнес-логики.
     * @param issues входящее значение issues, используемое бизнес-сценарием
     */
    public ClassValidationException(List<ValidationIssue> issues) {
        super("Класс не прошёл валидацию: " + issues.size() + " проблем(ы)");
        this.issues = issues;
    }
}
