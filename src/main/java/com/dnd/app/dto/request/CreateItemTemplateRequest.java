package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс CreateItemTemplateRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateItemTemplateRequest {

    @NotBlank(message = "Item template name is required")
    @Size(max = 100, message = "Item template name must not exceed 100 characters")
    private String name;

    private String description;

    private UUID itemTypeId;

    private String rarity;

    private String damageDice;

    private Integer damageBonus;

    private String damageType;

    private Boolean isStackable;

    private UUID skillId;

    private String skillActivation;
}
