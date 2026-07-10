package com.dnd.app.dto.content.grant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс CustomTextGrantPayload описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CustomTextGrantPayload",
        description = "Manual/custom grant and fallback for unknown grantType (grantType=CUSTOM_TEXT)")
public class CustomTextGrantPayload implements GrantPayload {

    @Schema(example = "Stormborn")
    private String title;

    @Schema(description = "Body text", example = "You can speak with creatures of the air.")
    private String body;

    @Schema(description = "Whether body is markdown", example = "false")
    private Boolean markdown;

    @Schema(description = "True => the player fills this in on their sheet", example = "false")
    private Boolean userEditable;
}
