package com.dnd.app.dto.featurerule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс CreateFeatureRuleRequest описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateFeatureRuleRequest {

    /** {@code FeatureRuleProfile} code, e.g. {@code action_cost}. */
    @NotBlank(message = "Тип правила обязателен")
    @Size(max = 48)
    private String ruleType;

    private Boolean enabled;

    private Integer sortOrder;

    @Size(max = 4000)
    private String notes;
}
