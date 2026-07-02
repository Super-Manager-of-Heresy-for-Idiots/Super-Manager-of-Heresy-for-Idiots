package com.dnd.app.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Admin correction of a spell's parsed resolution. Submitted from the spell-warning
 * review console: the confirmed saving-throw ability and/or attack-roll flag, plus
 * whether the record should remain flagged for review.
 */
@Data
@Schema(name = "SpellResolutionRequest")
public class SpellResolutionRequest {

    @Schema(description = "Saving-throw ability code (STRENGTH..CHARISMA); null/blank clears it", example = "DEXTERITY")
    private String saveAbility;

    @Schema(description = "Whether the spell resolves with an attack roll")
    private Boolean attackRoll;

    @Schema(description = "Keep the record flagged for review; typically false once corrected")
    private Boolean warning;
}
