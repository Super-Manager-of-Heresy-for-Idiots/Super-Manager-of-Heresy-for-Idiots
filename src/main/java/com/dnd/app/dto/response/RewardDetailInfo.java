package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Класс RewardDetailInfo описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RewardDetailInfo {
    // SKILL / class feature
    private String skillActivation;          // PASSIVE | ACTIVE (источник: Skill.skillType)
    private String damageDice;
    private Integer damageBonus;
    private String damageType;
    private String range;                    // нет в БД -> null
    private String duration;                 // нет в БД -> null
    private String usage;                    // нет в БД -> null
    private List<SkillEffectResponse> effects;

    // FEAT
    private String prerequisites;

    // ABILITY_SCORE_IMPROVEMENT
    private String abilityStatName;
    private Integer currentScore;
    private Integer maxScore;
    private List<AbilityOptionInfo> abilityOptions;  // варианты выбора для ASI
    private Integer asiPointsTotal;                   // сколько очков распределить (по правилам = 2)
}
