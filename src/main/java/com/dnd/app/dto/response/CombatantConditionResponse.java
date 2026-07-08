package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A live condition on a battle combatant, for the tracker/token badges. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombatantConditionResponse {
    private UUID conditionId;
    private String code;
    private String name;
    private String sourceText;
    /** Rounds left; null = until removed. */
    private Integer remainingRounds;
}
