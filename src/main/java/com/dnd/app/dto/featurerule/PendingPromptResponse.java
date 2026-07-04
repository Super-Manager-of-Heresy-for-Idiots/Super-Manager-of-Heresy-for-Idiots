package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/** A durable pending gameplay prompt (optional trigger/reaction awaiting a decision). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingPromptResponse {
    private UUID id;
    private UUID combatId;
    private UUID sourceFeatureId;
    private UUID featureTriggerId;
    private UUID triggerEventId;
    private String promptType;
    private String status;
    private Instant expiresAt;
    private Instant createdAt;
}
