package com.dnd.app.dto.request;

import com.dnd.app.domain.enums.StandardActionType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * A combatant takes a standard action (Dash / Dodge / Disengage / Help / Hide) on its own turn.
 * The action economy is spent through {@code slot} (default ACTION; some class features let these
 * be a bonus action). HELP requires {@code targetCombatantId} (the aided ally). HIDE resolves a
 * Stealth check — the actor supplies a manual {@code stealthD20} or omits it for the server to
 * roll; {@code stealthBonus} is the actor's Stealth modifier and {@code hideDc} the contest DC
 * (the highest enemy passive Perception, computed by the FE); if omitted the hide auto-succeeds
 * for the GM to adjudicate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StandardActionRequest {

    @NotNull(message = "Standard action type is required")
    private StandardActionType type;

    /** Action-economy slot to spend. Null → ACTION. */
    private SpendActionRequest.Slot slot;

    /** The aided ally for HELP. Required for HELP, ignored otherwise. */
    private UUID targetCombatantId;

    /** HIDE: manual Stealth d20; omit to have the server roll it. */
    @Min(value = 1, message = "stealthD20 must be between 1 and 20")
    @Max(value = 20, message = "stealthD20 must be between 1 and 20")
    private Integer stealthD20;

    /** HIDE: the actor's Stealth modifier (added to the d20). Null → 0. */
    private Integer stealthBonus;

    /** HIDE: contest DC (highest enemy passive Perception). Null → auto-succeed (GM adjudicates). */
    private Integer hideDc;
}
