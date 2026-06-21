package com.dnd.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * The combatant whose turn it currently is strikes a target. The attacker rolls their own d20
 * (tabletop style, like the initiative join); the server resolves hit/crit against the target's
 * AC and rolls the named attack's damage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleAttackRequest {

    @NotNull(message = "Target combatant ID is required")
    private UUID targetCombatantId;

    @NotBlank(message = "Attack name is required")
    private String attackName;

    @NotNull(message = "d20 is required")
    @Min(value = 1, message = "d20 must be between 1 and 20")
    @Max(value = 20, message = "d20 must be between 1 and 20")
    private Integer d20;
}
