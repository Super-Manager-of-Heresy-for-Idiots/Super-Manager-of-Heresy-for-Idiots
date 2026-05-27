package com.dnd.app.service.reward;

import com.dnd.app.domain.Subclass;
import com.dnd.app.dto.response.RewardDetailDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.SubclassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SubclassRewardResolver implements RewardResolver {

    private final SubclassRepository subclassRepository;

    @Override
    public String getSupportedType() {
        return "SUBCLASS";
    }

    @Override
    public RewardDetailDto resolve(UUID rewardId) {
        Subclass subclass = subclassRepository.findById(rewardId)
                .orElseThrow(() -> new ResourceNotFoundException("Subclass not found: " + rewardId));
        return RewardDetailDto.builder()
                .rewardId(subclass.getId())
                .name(subclass.getName())
                .description(subclass.getDescription())
                .build();
    }

    @Override
    public void validateRewardId(UUID rewardId) {
        if (!subclassRepository.existsById(rewardId)) {
            throw new ResourceNotFoundException("Subclass not found: " + rewardId);
        }
    }
}
