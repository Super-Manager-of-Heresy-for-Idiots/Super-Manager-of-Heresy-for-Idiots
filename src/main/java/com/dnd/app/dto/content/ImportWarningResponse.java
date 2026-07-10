package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс ImportWarningResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "ImportWarning", description = "A non-fatal content import warning")
public class ImportWarningResponse {
    private UUID id;
    private String sourceSlug;
    private String entityKind;
    private String entitySlug;
    private String warningCode;
    private String message;
    private Instant createdAt;
}
