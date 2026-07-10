package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс EnchantmentTypeResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnchantmentTypeResponse {
    private UUID id;
    private String name;
    private String description;
    private String damageDice;
    private Integer damageBonus;
    private String damageType;
    private BuffDebuffResponse buffDebuff;
}
