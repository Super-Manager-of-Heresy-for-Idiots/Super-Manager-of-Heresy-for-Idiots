package com.dnd.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Класс BattleCastSpellRequest описывает DTO входящего запроса, который переносит данные клиента в бизнес-сценарий.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleCastSpellRequest {

    @NotNull(message = "spellId is required")
    private UUID spellId;

    private UUID targetCombatantId;

    /**
     * AoE cast (Phase 2.3): every combatant covered by the template (from map's aoe-targets).
     * When present it wins over {@code targetCombatantId}; each target saves individually.
     */
    private java.util.List<UUID> targetCombatantIds;

    /** AoE template placement in grid cells (origin per AoeGeometry conventions) + rotation. */
    private Integer originX;
    private Integer originY;
    private Double rotationDeg;

    @Min(value = 0, message = "slotLevel must be 0-9")
    private Integer slotLevel;

    /**
     * How the spell's damage dice are resolved: {@code AUTO} (default) — the server rolls the plan's
     * dice; {@code MANUAL} — the player rolled physically and supplies the total in {@code manualDamage}.
     * Either way the server still applies the save-for-half and the target's resistance/immunity.
     */
    private String damageRollMode;

    /** The player-rolled dice total, when {@code damageRollMode = MANUAL} (pre-save, pre-resistance). */
    @Min(value = 0, message = "manualDamage must be >= 0")
    private Integer manualDamage;

    private UUID clientCommandId;
}
