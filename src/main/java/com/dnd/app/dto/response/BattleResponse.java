package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Full authoritative state of a battle: lifecycle, the group preview (average danger /
 * total xp) and the initiative-ordered tracker. Clients re-fetch this after WebSocket pings.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleResponse {

    private UUID id;
    private UUID campaignId;
    private String name;
    private String status;
    private Integer roundNumber;
    private Integer currentTurnIndex;
    private UUID currentCombatantId;

    // --- group preview ---
    private Integer monsterCount;
    private BigDecimal averageDanger;
    private Integer totalXp;
    private Integer overrideXp;

    private List<BattleCombatantResponse> combatants;

    private Instant startedAt;
    private Instant endedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
