package com.dnd.app.exception;

import com.dnd.app.dto.content.ValidationIssue;
import lombok.Getter;

import java.util.List;

/**
 * Carries structured validation issues for the class-authoring aggregate. Mapped to
 * HTTP 422 with the issue list in the response body.
 */
@Getter
public class ClassValidationException extends RuntimeException {

    private final transient List<ValidationIssue> issues;

    public ClassValidationException(List<ValidationIssue> issues) {
        super("Класс не прошёл валидацию: " + issues.size() + " проблем(ы)");
        this.issues = issues;
    }
}
