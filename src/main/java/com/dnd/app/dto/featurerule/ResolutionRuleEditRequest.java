package com.dnd.app.dto.featurerule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс ResolutionRuleEditRequest описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolutionRuleEditRequest {

    /** {@code saving_throw} | {@code ability_check} | {@code skill_check} | {@code attack_roll} | {@code contested_check}. */
    @NotBlank
    @Size(max = 24)
    private String resolutionType;

    private UUID abilityId;
    private UUID skillId;

    /** DSL DC expression (e.g. {@code 8+proficiency_bonus+ability_mod("WIS")}); blank = none. */
    @Size(max = 2000)
    private String dcFormula;
}
