package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Roll ONE shared initiative die for a group of combatants (Phase 2.4): all listed combatants take the
 * same d20 (each keeps its own bonus), then the tracker re-sorts. GM/admin only.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupInitiativeRequest {

    @NotEmpty(message = "combatantIds must not be empty")
    private List<UUID> combatantIds;
}
