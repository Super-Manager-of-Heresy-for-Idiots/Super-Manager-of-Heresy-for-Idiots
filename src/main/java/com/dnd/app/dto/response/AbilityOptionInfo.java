package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Одна характеристика как вариант для ASI: текущее значение и потолок.
 * Список таких опций приходит в RewardDetailInfo.abilityOptions для награды
 * типа ABILITY_SCORE_IMPROVEMENT.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AbilityOptionInfo {
    private UUID statTypeId;
    private String name;
    private Integer currentScore;
    private Integer maxScore;
}
