package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemTypeResponse {
    private UUID id;
    private String name;
    private String description;
    private String slot;
    private String damageDice;
    private Integer damageBonus;
    private String damageType;
    private UUID skillId;
    private String skillName;
    private String skillActivation;
}
