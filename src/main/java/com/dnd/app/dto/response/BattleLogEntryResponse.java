package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * One combat-log entry (Phase 1.2). {@code payload} is the parsed JSON object (formula, dice,
 * modifier, …) so the UI can expand a result without re-parsing a string. Non-GM callers never
 * receive GM_ONLY rows — they are filtered server-side in the log API.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleLogEntryResponse {
    private UUID id;
    private long seq;
    private String type;
    private UUID actorCombatantId;
    private UUID targetCombatantId;
    /** Parsed JSON payload (may be null). */
    private Object payload;
    private String visibility;
    private Instant createdAt;
}
