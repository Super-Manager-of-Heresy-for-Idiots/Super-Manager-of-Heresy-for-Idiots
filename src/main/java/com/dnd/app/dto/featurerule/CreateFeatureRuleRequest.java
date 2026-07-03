package com.dnd.app.dto.featurerule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Create a new manually-authored rule for a class feature. New rules always start in {@code draft};
 * status changes go through the dedicated approve/disable endpoints.
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
