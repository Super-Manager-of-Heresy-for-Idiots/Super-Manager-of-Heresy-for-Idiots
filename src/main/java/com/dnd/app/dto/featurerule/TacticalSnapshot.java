package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Tactical projection of a character's feature-runtime state for the frontend / map-service: the active
 * transformation (if any) and companions. The map-service consumes this snapshot; it does not read the
 * feature tables or interpret rules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TacticalSnapshot {
    private UUID characterId;
    private UUID activeTransformationMonsterId;
    private UUID activeTransformationId;
    private List<CompanionResponse> companions;
}
