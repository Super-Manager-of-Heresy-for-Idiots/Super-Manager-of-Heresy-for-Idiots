package com.dnd.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GM adjustment of a combatant's action-economy maxima during a battle. Each field is optional;
 * only the provided pools are changed. Used to model actions/bonus actions growing with level or
 * spells, and to grant legendary actions. Spent counters are clamped to the new maxima.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustActionEconomyRequest {

    @Min(0)
    @Max(20)
    private Integer actionMax;

    @Min(0)
    @Max(20)
    private Integer bonusActionMax;

    @Min(0)
    @Max(20)
    private Integer legendaryActionMax;
}
