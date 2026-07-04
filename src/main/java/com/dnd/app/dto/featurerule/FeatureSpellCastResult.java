package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Result of casting a spell via a feature grant (free cast / resource spend). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureSpellCastResult {
    private UUID spellGrantId;
    private UUID spellId;
    private boolean castWithoutSlot;
    private String resourceKey;
    private Integer resourceSpent;
    private Integer resourceRemaining;
    private String message;
}
