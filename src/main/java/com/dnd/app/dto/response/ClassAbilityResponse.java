package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс ClassAbilityResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassAbilityResponse {
    private String name;
    private Integer level;
    private String className;
    private String description;
    private String activationType;
    private boolean attackRoll;
    private String saveAbility;
    private boolean usableAsAttack;
    /** Damage expression when usable as an attack (e.g. "1d6"); null otherwise. */
    private String damage;
    private String damageType;
    private String healingDice;
    private Integer healingFlat;
    private boolean warning;
}
