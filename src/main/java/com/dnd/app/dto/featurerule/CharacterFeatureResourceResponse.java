package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** A character's live feature-resource counter for the sheet / GM tools. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterFeatureResourceResponse {
    private UUID id;
    private UUID resourceDefinitionId;
    private String resourceKey;
    private String displayName;
    private Integer currentValue;
    private Integer maxValue;
    private String sharedPoolKey;
    private boolean allowNegative;
    private Instant lastResetAt;
}
