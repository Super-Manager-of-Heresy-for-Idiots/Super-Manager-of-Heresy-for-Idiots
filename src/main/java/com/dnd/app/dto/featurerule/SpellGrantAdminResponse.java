package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A SPELL_GRANT for the admin editor. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpellGrantAdminResponse {
    private UUID id;
    private UUID spellId;
    private boolean countsAgainstKnown;
    private boolean alwaysPrepared;
    private boolean castWithoutSlot;
    private UUID spellcastingAbilityOverrideId;
}
