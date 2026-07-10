package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс ItemTypeResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemTypeResponse {
    private UUID id;
    private String name;
    private String description;
    private String slot;
    private String damageDice;
    private Integer damageBonus;
    private String damageType;
    private UUID skillId;
    private String skillName;
    private String skillActivation;
}
