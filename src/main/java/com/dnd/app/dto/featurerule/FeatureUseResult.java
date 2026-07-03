package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Result of using a feature. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureUseResult {
    private UUID featureId;
    private String featureName;
    private String actionType;
    private String resourceKey;
    private Integer resourceSpent;
    private Integer resourceRemaining;
    private UUID logId;
    private String message;
}
