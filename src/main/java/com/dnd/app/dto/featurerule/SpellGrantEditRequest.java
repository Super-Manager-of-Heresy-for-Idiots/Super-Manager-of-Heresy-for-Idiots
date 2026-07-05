package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Admin edit of a SPELL_GRANT (specific spell + prepared/known/free-cast + ability override). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpellGrantEditRequest {
    private UUID spellId;
    private boolean countsAgainstKnown;
    private boolean alwaysPrepared;
    private boolean castWithoutSlot;
    private UUID spellcastingAbilityOverrideId;
}
