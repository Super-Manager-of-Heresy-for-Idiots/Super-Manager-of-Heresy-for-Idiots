package com.dnd.app.dto.featurerule;

import com.dnd.app.dto.response.SpellSlotsResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Outcome of casting a spell through the feature-rules runtime. */
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

    private String message;
}
