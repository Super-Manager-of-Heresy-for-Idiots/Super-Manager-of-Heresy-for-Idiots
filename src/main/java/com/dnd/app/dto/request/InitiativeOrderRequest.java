package com.dnd.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Full replacement of the initiative values for a battle (GM quick tool, Phase 1.7). Every combatant
 * in the battle must be listed exactly once; the server sets each initiative, re-sorts the tracker
 * and keeps the turn anchored on whoever is currently acting.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitiativeOrderRequest {

    @NotEmpty(message = "entries must not be empty")
    @Valid
    private List<Entry> entries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Entry {
        @NotNull(message = "combatantId is required")
        private UUID combatantId;

        @NotNull(message = "initiative is required")
        private Integer initiative;
    }
}
