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
public class SkillResponse {
    private UUID id;
    private String name;
    private String description;
    private String skillType;
    private String damageDice;
    private Integer damageBonus;
    private String damageType;
    private List<SkillEffectResponse> effects;
    private Instant createdAt;
    private Instant updatedAt;
}
