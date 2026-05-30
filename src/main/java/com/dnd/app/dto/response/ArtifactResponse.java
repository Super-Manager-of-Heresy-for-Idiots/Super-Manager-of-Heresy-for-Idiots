package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactResponse {
    private UUID id;
    private String name;
    private String description;
    private UUID itemTypeId;
    private String itemTypeName;
    private String itemTypeSlot;
    private String rarity;
    private String properties;
    private String specialAbilities;
    private UUID createdById;
    private UUID teamId;
    private Instant createdAt;
}
