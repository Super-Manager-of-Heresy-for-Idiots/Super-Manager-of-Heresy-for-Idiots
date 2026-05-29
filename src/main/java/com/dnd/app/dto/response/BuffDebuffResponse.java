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
public class BuffDebuffResponse {
    private UUID id;
    private String name;
    private String description;
    private String effectType;
    private UUID targetStatId;
    private String targetStatName;
    private Integer modifierValue;
    private Integer durationRounds;
    private Boolean isBuff;
    private Instant createdAt;
}
