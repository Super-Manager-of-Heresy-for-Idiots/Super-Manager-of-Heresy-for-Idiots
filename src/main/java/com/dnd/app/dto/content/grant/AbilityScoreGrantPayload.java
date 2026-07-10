package com.dnd.app.dto.content.grant;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс AbilityScoreGrantPayload описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
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
