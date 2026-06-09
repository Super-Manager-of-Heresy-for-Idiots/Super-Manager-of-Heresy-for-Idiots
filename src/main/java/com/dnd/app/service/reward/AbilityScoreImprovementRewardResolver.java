package com.dnd.app.service.reward;

import com.dnd.app.domain.StatType;
import com.dnd.app.dto.response.RewardDetailDto;
import com.dnd.app.dto.response.RewardDetailInfo;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.StatTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class AbilityScoreImprovementRewardResolver implements RewardResolver {

    private final StatTypeRepository statTypeRepository;

    @Override
    public String getSupportedType() {
        return "ABILITY_SCORE_IMPROVEMENT";
    }

    @Override
    public RewardDetailDto resolve(UUID rewardId) {
        StatType statType = statTypeRepository.findById(rewardId)
                .orElseThrow(() -> new ResourceNotFoundException("Характеристика не найдена: " + rewardId));
        // currentScore зависит от персонажа и заполняется в LevelUpService;
        // maxScore — потолок характеристики по правилам системы.
        RewardDetailInfo detail = RewardDetailInfo.builder()
                .abilityStatName(statType.getName())
                .maxScore(20)
                .build();
        return RewardDetailDto.builder()
                .rewardId(statType.getId())
                .name(statType.getName())
                .description("Увеличение " + statType.getName() + " на 1")
                .detail(detail)
                .build();
    }

    @Override
    public void validateRewardId(UUID rewardId) {
        if (!statTypeRepository.existsById(rewardId)) {
            throw new ResourceNotFoundException("Характеристика не найдена: " + rewardId);
        }
    }
}
