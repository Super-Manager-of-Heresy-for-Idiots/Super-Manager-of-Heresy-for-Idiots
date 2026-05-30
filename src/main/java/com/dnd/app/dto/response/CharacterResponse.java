package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterResponse {
    private UUID id;
    private String name;
    private Integer totalLevel;
    private Long experience;
    private List<ClassLevelResponse> classLevels;
    private CharacterRaceResponse race;
    private UUID teamId;
    private String teamName;
    private UUID ownerId;
    private String ownerUsername;
    private List<CharacterStatResponse> stats;
    private List<InventorySlotResponse> inventorySlots;
    private Instant createdAt;
    private Instant updatedAt;
}
