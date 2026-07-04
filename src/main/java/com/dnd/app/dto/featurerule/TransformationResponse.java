package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** A character's active/ended transformation. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransformationResponse {
    private UUID id;
    private UUID monsterId;
    private UUID sourceFeatureId;
    private UUID activeEffectId;
    private String status;
    private Instant startedAt;
    private Instant expiresAt;
}
