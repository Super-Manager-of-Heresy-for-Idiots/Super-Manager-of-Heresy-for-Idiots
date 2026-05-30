package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AbilityCheckResponse {
    private String statName;
    private int baseValue;
    private int modifier;
    private int buffBonus;
    private int equipmentBonus;
    private int totalModifier;
}
