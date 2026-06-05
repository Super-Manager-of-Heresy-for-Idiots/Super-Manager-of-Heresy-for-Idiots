package com.dnd.app.service.reward;

import com.dnd.app.domain.BuffDebuff;
import com.dnd.app.dto.response.RewardDetailDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.BuffDebuffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BuffDebuffRewardResolver implements RewardResolver {

    private final BuffDebuffRepository buffDebuffRepository;

    @Override
    public String getSupportedType() {
        return "BUFF_DEBUFF";
    }

    @Override
    public RewardDetailDto resolve(UUID rewardId) {
        BuffDebuff buffDebuff = buffDebuffRepository.findById(rewardId)
                .orElseThrow(() -> new ResourceNotFoundException("Бафф/дебафф не найден: " + rewardId));
        return RewardDetailDto.builder()
                .rewardId(buffDebuff.getId())
                .name(buffDebuff.getName())
                .description(buffDebuff.getDescription())
                .build();
    }

    @Override
    public void validateRewardId(UUID rewardId) {
        if (!buffDebuffRepository.existsById(rewardId)) {
            throw new ResourceNotFoundException("Бафф/дебафф не найден: " + rewardId);
        }
    }
}
