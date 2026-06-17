package com.dnd.app.dto.content.grant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * NUMERIC_MODIFIER grant — a numeric modifier keyed by an open text key
 * (homebrew flexibility). Known keys are suggested but not enforced.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "NumericModifierGrantPayload",
        description = "Numeric modifier (payload for grantType=NUMERIC_MODIFIER)")
public class NumericModifierGrantPayload implements GrantPayload {

    @Schema(description = "Open key. Known: speed | ac | hp_max | initiative | ...", example = "speed")
    private String modifierKey;

    @Schema(description = "Amount (may be negative)", example = "10")
    private Integer amount;

    @Schema(description = "Unit text", example = "ft")
    private String unitText;

    @Schema(description = "Free-text duration")
    private String durationText;

    @Schema(description = "Whether this modifier stacks with others")
    private Boolean stacking;
}
