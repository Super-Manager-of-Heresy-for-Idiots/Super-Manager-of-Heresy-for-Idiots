package com.dnd.app.service.reward;

import com.dnd.app.dto.response.RewardDetailDto;
import com.dnd.app.dto.response.RewardDetailInfo;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * ASI — параметрическая награда: она не ссылается на конкретную характеристику
 * (reward_id у таких записей = NULL). Резолвер возвращает описание правила;
 * список доступных характеристик (abilityOptions) и применение выбора
 * заполняются в LevelUpService, т.к. зависят от персонажа.
 */
@Component
public class AbilityScoreImprovementRewardResolver implements RewardResolver {

    public static final int ASI_POINTS_TOTAL = 2;
    public static final int ASI_MAX_SCORE = 20;

    @Override
    public String getSupportedType() {
        return "ABILITY_SCORE_IMPROVEMENT";
    }

    @Override
    public RewardDetailDto resolve(UUID rewardId) {
        RewardDetailInfo detail = RewardDetailInfo.builder()
                .asiPointsTotal(ASI_POINTS_TOTAL)
                .maxScore(ASI_MAX_SCORE)
                .build();
        return RewardDetailDto.builder()
                .rewardId(null)
                .name("Увеличение характеристик")
                .description("Повысьте одну характеристику на 2 или две характеристики на 1 (не выше "
                        + ASI_MAX_SCORE + ")")
                .detail(detail)
                .build();
    }

    @Override
    public void validateRewardId(UUID rewardId) {
        // У ASI нет целевой сущности — валидировать нечего.
    }
}
