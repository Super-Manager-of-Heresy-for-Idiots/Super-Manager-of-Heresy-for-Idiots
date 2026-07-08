package com.dnd.app.dto.request;

import com.dnd.app.domain.enums.AttackRollMode;
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
 *
 * <p>Roll mode: for ADVANTAGE/DISADVANTAGE supply two manual dice ({@code d20A}/{@code d20B}) and
 * the server keeps the higher/lower; omit all dice to have the server roll virtually. The legacy
 * single {@code d20} is still accepted for NORMAL. {@code advantageReason} is a frontend-supplied
 * hint (e.g. high-ground) recorded as-is — it is NOT independently validated by core BE in this phase.
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

    /** NORMAL (default), ADVANTAGE or DISADVANTAGE. Null is treated as NORMAL. */
    private AttackRollMode rollMode;

    /** Legacy single manual d20 (NORMAL). Omit to roll virtually. */
    @Min(value = 1, message = "d20 must be between 1 and 20")
    @Max(value = 20, message = "d20 must be between 1 and 20")
    private Integer d20;

    /** First manual die for ADVANTAGE/DISADVANTAGE. */
    @Min(value = 1, message = "d20A must be between 1 and 20")
    @Max(value = 20, message = "d20A must be between 1 and 20")
    private Integer d20A;

    /** Second manual die for ADVANTAGE/DISADVANTAGE. */
    @Min(value = 1, message = "d20B must be between 1 and 20")
    @Max(value = 20, message = "d20B must be between 1 and 20")
    private Integer d20B;

    /** Frontend-supplied reason, e.g. HIGH_GROUND_RANGED_ATTACK | GM_OVERRIDE | SPELL_EFFECT | OTHER. */
    private String advantageReason;

    // ---- Saving throw (for save-based attacks: the TARGET rolls, not the attacker) --------------

    /** Roll mode for the target's saving throw. Null → NORMAL. Same rules as the attack roll. */
    private AttackRollMode saveRollMode;

    /** Legacy single manual save d20 (NORMAL). Omit to have the server roll the save virtually. */
    @Min(value = 1, message = "saveD20 must be between 1 and 20")
    @Max(value = 20, message = "saveD20 must be between 1 and 20")
    private Integer saveD20;

    /** First manual save die for ADVANTAGE/DISADVANTAGE. */
    @Min(value = 1, message = "saveD20A must be between 1 and 20")
    @Max(value = 20, message = "saveD20A must be between 1 and 20")
    private Integer saveD20A;

    /** Second manual save die for ADVANTAGE/DISADVANTAGE. */
    @Min(value = 1, message = "saveD20B must be between 1 and 20")
    @Max(value = 20, message = "saveD20B must be between 1 and 20")
    private Integer saveD20B;
}
