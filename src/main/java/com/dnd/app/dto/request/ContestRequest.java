package com.dnd.app.dto.request;

import com.dnd.app.domain.enums.ContestType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * An opposed melee contest on the actor's turn: GRAPPLE or SHOVE against a target. Both sides make
 * a check (d20 + bonus); supply a manual {@code attackerD20}/{@code targetD20} or omit either for
 * the server to roll it. Bonuses are the Athletics/Acrobatics modifiers the FE reads off the sheets.
 * SHOVE resolves to {@code shoveMode} PRONE (default) — pushing 5 ft is forced movement (Phase 2.12).
 * The defender wins ties (5e).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContestRequest {

    @NotNull(message = "Contest type is required")
    private ContestType type;

    @NotNull(message = "Target combatant ID is required")
    private UUID targetCombatantId;

    /** Manual attacker d20 (Athletics); omit for the server to roll. */
    @Min(value = 1, message = "attackerD20 must be between 1 and 20")
    @Max(value = 20, message = "attackerD20 must be between 1 and 20")
    private Integer attackerD20;

    /** Attacker's Athletics modifier. Null → 0. */
    private Integer attackerBonus;

    /** Manual target d20 (Athletics or Acrobatics); omit for the server to roll. */
    @Min(value = 1, message = "targetD20 must be between 1 and 20")
    @Max(value = 20, message = "targetD20 must be between 1 and 20")
    private Integer targetD20;

    /** Target's chosen defence modifier (Athletics or Acrobatics). Null → 0. */
    private Integer targetBonus;

    /** SHOVE only: PRONE (default) knocks the target prone; PUSH is deferred to forced movement (2.12). */
    private String shoveMode;
}
