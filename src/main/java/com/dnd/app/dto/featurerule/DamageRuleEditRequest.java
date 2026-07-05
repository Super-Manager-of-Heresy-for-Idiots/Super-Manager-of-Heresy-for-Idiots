package com.dnd.app.dto.featurerule;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Admin edit of a feature DAMAGE rule (dice / flat formulas + damage type + attack/save gating). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DamageRuleEditRequest {

    /** DSL dice expression (e.g. {@code 2d6} or {@code ceil(class_level("rogue")/2)d6}); blank = none. */
    @Size(max = 2000)
    private String diceFormula;

    /** DSL flat-amount expression (e.g. {@code ability_mod("STR")}); blank = none. */
    @Size(max = 2000)
    private String flatFormula;

    private UUID damageTypeId;
    private boolean requiresAttackHit;
    private boolean requiresSave;
    private boolean halfOnSave;
}
