package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A feature the character can use as an action, with its action/resource cost and availability. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableFeatureAction {
    private UUID featureId;
    private String featureName;
    private UUID featureRuleId;
    private String actionType;
    private String actionTypeLabel;
    private UUID resourceDefinitionId;
    private String resourceKey;
    private Integer resourceCost;
    private Integer resourceRemaining;
    private boolean available;
    private String unavailableReason;
    private boolean requiresTarget;
    private boolean requiresConfirmation;
}
