package com.dnd.app.dto.request;

import com.dnd.app.domain.enums.NpcRole;
import com.dnd.app.domain.enums.NpcSourceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNpcRequest {

    @Size(max = 100, message = "NPC name must not exceed 100 characters")
    private String name;

    private String publicDescription;

    private String privateDescription;

    private Boolean isVisibleToPlayers;

    // Optional: switch the authoring mode. When provided, the corresponding
    // build fields are (re)validated.
    private NpcSourceType sourceType;

    // Optional: update the NPC's world role (merchant, quest giver, …).
    private NpcRole npcRole;

    // --- CLASS_BASED build fields ---
    private UUID raceId;

    private UUID classId;

    @Min(value = 1, message = "NPC level must be at least 1")
    private Integer level;

    private String abilities;

    // When non-null, replaces the NPC's spell selection wholesale.
    private List<UUID> spellIds;

    // --- MONSTER_BASED build field ---
    private UUID sourceMonsterId;
}
