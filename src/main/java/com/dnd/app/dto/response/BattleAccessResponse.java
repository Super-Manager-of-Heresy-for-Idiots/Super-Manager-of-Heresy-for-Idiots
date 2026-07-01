package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Service-to-service answer to "what may this user do in this battle?". Consumed by map-service
 * so it can authorize token control without reaching into the core database or duplicating the
 * combat permission rules. Combat authority stays in core BE; this is a read-only projection.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleAccessResponse {

    private UUID battleId;
    private UUID campaignId;
    private UUID userId;
    private boolean canView;
    private boolean canManageBattle;
    private boolean canControlAnyCombatant;
    private List<UUID> controllableCombatantIds;
    private List<UUID> controllableCharacterIds;
}
