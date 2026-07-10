package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс CharacterClassFeatureResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CharacterClassFeature", description = "A structured class feature the character has")
public class CharacterClassFeatureResponse {
    private UUID id;
    private UUID classId;
    private String className;
    private Integer level;
    private String title;
    private String description;
    private String activationType;
}
