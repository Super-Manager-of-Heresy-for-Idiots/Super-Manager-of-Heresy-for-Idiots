package com.dnd.app.dto.content.grant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс SubclassGrantPayload описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "SubclassGrantPayload", description = "Grants a subclass (payload for grantType=SUBCLASS)")
public class SubclassGrantPayload implements GrantPayload {

    @Schema(description = "Existing subclass id of this class")
    private UUID subclassId;

    @Schema(description = "Reference to a subclass created in the same write request (by client key)")
    private String subclassKey;

    @Schema(description = "Resolved subclass label (read responses)")
    private com.dnd.app.dto.content.ContentLabelDto subclass;
}
