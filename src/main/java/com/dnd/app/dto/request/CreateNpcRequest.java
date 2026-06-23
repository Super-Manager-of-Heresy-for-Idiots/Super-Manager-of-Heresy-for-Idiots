package com.dnd.app.dto.request;

import com.dnd.app.domain.enums.NpcRole;
import com.dnd.app.domain.enums.NpcSourceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
public class CreateNpcRequest {

    @NotBlank(message = "NPC name is required")
    @Size(max = 100, message = "NPC name must not exceed 100 characters")
    private String name;

    private String publicDescription;

    private String privateDescription;

    private Boolean isVisibleToPlayers;

    // Mutually exclusive authoring mode. NULL => legacy free-form NPC.
    private NpcSourceType sourceType;

    // World role (merchant, quest giver, …). NULL => unspecified.
    private NpcRole npcRole;

    // --- CLASS_BASED (required when sourceType == CLASS_BASED) ---
    private UUID raceId;

    private UUID classId;

    @Min(value = 1, message = "NPC level must be at least 1")
    private Integer level;

    // Optional, progression-independent abilities/spells.
    private String abilities;

    private List<UUID> spellIds;

    // --- MONSTER_BASED (required when sourceType == MONSTER_BASED) ---
    private UUID sourceMonsterId;
}
