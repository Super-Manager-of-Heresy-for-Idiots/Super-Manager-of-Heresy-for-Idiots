package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A MONSTER_FORM (Wild Shape) filter for the admin editor. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonsterFormAdminResponse {
    private UUID id;
    private String creatureType;
    private String maxCrFormula;
    private String maxCrFormulaStatus;
    private String maxCrFormulaMessage;
    private String movementRestriction;
    private String sizeFilter;
    private String sourceFilter;
}
