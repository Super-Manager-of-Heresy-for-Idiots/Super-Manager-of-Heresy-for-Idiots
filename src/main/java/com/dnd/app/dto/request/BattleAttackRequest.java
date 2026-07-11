package com.dnd.app.dto.request;

import com.dnd.app.domain.enums.AttackRollMode;
import com.dnd.app.domain.enums.CoverType;
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
 * Класс BattleAttackRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
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

    // ---- Range / reach validation (Phase 2.5) ---------------------------------------------------
    // Grid squares of both tokens, supplied by the FE (positions are map-authoritative and relayed
    // here on demand — core stores none, per A1). When all four are present the server gates the
    // attack by distance (Chebyshev × 5 ft): a melee strike beyond reach or a shot beyond long range
    // is rejected; long range and shooting while an enemy threatens melee force DISADVANTAGE. Omit
    // them to skip the range gate (backward compatible). {@code gmOverrideRange} bypasses the gate
    // AND any forced disadvantage, with a note in the log.

    /** Attacker token column (grid square). Range gate is applied only when all four coords are present. */
    private Integer attackerCol;
    /** Attacker token row (grid square). */
    private Integer attackerRow;
    /** Target token column (grid square). */
    private Integer targetCol;
    /** Target token row (grid square). */
    private Integer targetRow;

    /** FE hint: an enemy is within melee reach of this (ranged) attacker → ranged-in-melee disadvantage. */
    private Boolean attackerInMeleeThreat;

    /**
     * Высоты токенов в футах для 3D-дистанции (фаза 2.13). Когда обе заданы, дальность считается с учётом
     * разницы высот (Chebyshev по X/Y/высоте) — важно для летающих целей. Null → 2D-дистанция как раньше.
     */
    private Integer attackerElevationFt;
    private Integer targetElevationFt;

    /** GM bypass of the range gate and any range-derived disadvantage (recorded in the log). */
    private Boolean gmOverrideRange;

    // ---- Opportunity / reaction attack (Phase 2.8) ----------------------------------------------
    /**
     * When true this is a reaction strike (e.g. an opportunity attack) made out of turn: it is
     * resolved for {@code attackerCombatantId} and spends that combatant's reaction instead of the
     * active combatant's action. Null/false → a normal attack by the active combatant.
     */
    private Boolean reaction;

    /** The reacting attacker for a reaction strike; required when {@code reaction} is true. */
    private UUID attackerCombatantId;

    // ---- Cover (Phase 2.6) ----------------------------------------------------------------------
    /**
     * The target's cover, chosen manually by the attacker. HALF/THREE_QUARTERS raise the target's AC
     * (and Dexterity saving throws) by +2/+5; TOTAL rejects the attack (cannot be targeted). Null → NONE.
     */
    private CoverType cover;
}
