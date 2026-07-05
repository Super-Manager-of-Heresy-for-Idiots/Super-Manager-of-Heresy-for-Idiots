package com.dnd.app.dto.featurerule;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Admin edit of a MONSTER_FORM filter (creature type, max CR, movement/size/source constraints). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonsterFormEditRequest {

    @Size(max = 32)
    private String creatureType;

    /** DSL max-CR expression (e.g. {@code max(0.25, floor(class_level("druid")/3))}); blank = none. */
    @Size(max = 2000)
    private String maxCrFormula;

    @Size(max = 24)
    private String movementRestriction;

    @Size(max = 24)
    private String sizeFilter;

    @Size(max = 64)
    private String sourceFilter;
}
