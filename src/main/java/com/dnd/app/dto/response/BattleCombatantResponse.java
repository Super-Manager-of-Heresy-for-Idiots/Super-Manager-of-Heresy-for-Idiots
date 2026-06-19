package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * A single entry on the battle tracker. {@code monsterId} or {@code characterId} is populated
 * depending on {@code type}; {@code ownerUserId} is the controlling player for characters.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleCombatantResponse {

    private UUID id;
    private String type;
    private String displayName;
    private UUID monsterId;
    private UUID characterId;
    private UUID ownerUserId;
    private Integer instanceIndex;
    private Integer initiative;
    private Integer initiativeRoll;
    private Integer turnOrder;
    private Integer currentHp;
    private Integer maxHp;
    private boolean currentTurn;
}
