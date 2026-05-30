package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItemTemplateResponse {
    private UUID id;
    private String name;
    private String description;
    private String itemTypeName;
    private String rarity;
    private String damageDice;
    private Integer damageBonus;
    private String damageType;
    private Boolean isStackable;
    private String skillName;
    private String skillActivation;
    private String sourceHomebrewTitle;
}
