package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A monster form a character knows. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnownFormResponse {
    private UUID id;
    private UUID monsterId;
    private UUID sourceFeatureId;
    private Integer learnedAtLevel;
    private boolean approvedByDm;
}
