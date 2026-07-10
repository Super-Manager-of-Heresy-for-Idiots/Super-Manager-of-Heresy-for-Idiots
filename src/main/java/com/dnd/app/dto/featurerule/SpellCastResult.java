package com.dnd.app.dto.featurerule;

import com.dnd.app.dto.response.SpellSlotsResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс SpellCastResult описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpellCastResult {

    private UUID spellId;
    private String spellName;

    /** Slot level actually spent; null for cantrips. */
    private Integer slotLevelUsed;

    /** Action-economy code spent in combat (action/bonus_action/reaction), null when none applied. */
    private String actionType;

    /** Active effects created on the effect target. */
    private int effectsApplied;

    /** What to roll (damage dice, DC, healing) — same structure the feature plan endpoint returns. */
    private FeatureExecutionPlan plan;

    /** Slot state after the cast (null for cantrips — nothing changed). */
    private SpellSlotsResponse slots;

    /**
     * Final damage actually dealt to the battle target after save-for-half and resistance/immunity
     * (Phase 2.1b/2.1c). Null when the cast was not in a battle or dealt no damage to a target.
     */
    private Integer appliedDamage;

    /** Damage modifier applied to the target: NONE / RESISTED / IMMUNE / VULNERABLE. Null when no damage. */
    private String appliedDamageModifier;

    private String message;
}
