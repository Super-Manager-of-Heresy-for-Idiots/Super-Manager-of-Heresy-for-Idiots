package com.dnd.app.service.reward;

import com.dnd.app.domain.Feat;
import com.dnd.app.dto.response.RewardDetailDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.FeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FeatRewardResolver implements RewardResolver {

    private final FeatRepository featRepository;

    @Override
    public String getSupportedType() {
        return "FEAT";
    }

    @Override
    public RewardDetailDto resolve(UUID rewardId) {
        Feat feat = featRepository.findById(rewardId)
                .orElseThrow(() -> new ResourceNotFoundException("Feat not found: " + rewardId));
        return RewardDetailDto.builder()
                .rewardId(feat.getId())
                .name(feat.getName())
                .description(feat.getDescription())
                .build();
    }

    @Override
    public void validateRewardId(UUID rewardId) {
        if (!featRepository.existsById(rewardId)) {
            throw new ResourceNotFoundException("Feat not found: " + rewardId);
        }
    }
}
