package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Result of a feature-runtime cleanup pass (Stage 13). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureMaintenanceResult {
    private int expiredEffects;
    private int expiredPrompts;
    private int endedStaleTransformations;
}
