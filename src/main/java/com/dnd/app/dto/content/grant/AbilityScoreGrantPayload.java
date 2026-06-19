package com.dnd.app.dto.content.grant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * ABILITY_SCORE grant — ASI / fixed ability bonus.
 * Classic ASI is two options of one CHOICE group:
 * "+2 to one" {@code (chooseCount=1, bonusPerChoice=2, maxPerAbility=2)} and
 * "+1 to two" {@code (chooseCount=2, bonusPerChoice=1, maxPerAbility=1)}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "AbilityScoreGrantPayload",
        description = "Ability score increase (payload for grantType=ABILITY_SCORE)")
public class AbilityScoreGrantPayload implements GrantPayload {

    @Schema(description = "Allowed abilities (ability_score ids); empty/null => any")
    private List<UUID> abilityOptionIds;

    @Schema(description = "How many DISTINCT abilities to increase", example = "1")
    private Integer chooseCount;

    @Schema(description = "Bonus applied per chosen ability", example = "2")
    private Integer bonusPerChoice;

    @Schema(description = "Validation total (chooseCount * bonusPerChoice)", example = "2")
    private Integer totalBonus;

    @Schema(description = "Max bonus to a single ability from this grant", example = "2")
    private Integer maxPerAbility;

    @Schema(description = "Hard ceiling for an ability score (usually 20)", example = "20")
    private Integer maxScore;
}
