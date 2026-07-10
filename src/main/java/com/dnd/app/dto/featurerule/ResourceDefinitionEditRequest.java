package com.dnd.app.dto.featurerule;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс ResourceDefinitionEditRequest описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceDefinitionEditRequest {

    @Size(max = 64)
    private String resourceKey;

    @Size(max = 120)
    private String displayName;

    /** DSL expression for the max value (blank = no formula / manual max). */
    @Size(max = 2000)
    private String maxFormula;

    /** rest_type code the resource resets on (e.g. short_rest, long_rest); blank = no reset. */
    @Size(max = 32)
    private String resetRestType;

    /** Optional partial-rest recovery formula. Blank means reset fully restores to max. */
    @Size(max = 2000)
    private String resetAmountFormula;

    /** Optional spend-per-use formula for action integrations. */
    @Size(max = 2000)
    private String spendPerUseFormula;

    private boolean allowNegative;

    @Size(max = 64)
    private String sharedPoolKey;
}
