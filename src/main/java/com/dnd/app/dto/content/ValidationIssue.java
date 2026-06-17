package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single structured validation issue, addressed by a JSON path into the request so
 * the UI can attach a badge to the offending group/option/grant.
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

    public static ValidationIssue error(String path, String code, String message) {
        return ValidationIssue.builder().path(path).code(code).severity("ERROR").message(message).build();
    }

    public static ValidationIssue warning(String path, String code, String message) {
        return ValidationIssue.builder().path(path).code(code).severity("WARNING").message(message).build();
    }

    public boolean isError() {
        return "ERROR".equals(severity);
    }
}
