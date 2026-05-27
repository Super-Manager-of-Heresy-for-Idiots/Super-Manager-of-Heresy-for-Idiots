package com.dnd.app.service.reward;

import com.dnd.app.domain.Skill;
import com.dnd.app.dto.response.RewardDetailDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SkillRewardResolver implements RewardResolver {

    private final SkillRepository skillRepository;

    @Override
    public String getSupportedType() {
        return "SKILL";
    }

    @Override
    public RewardDetailDto resolve(UUID rewardId) {
        Skill skill = skillRepository.findById(rewardId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + rewardId));
        return RewardDetailDto.builder()
                .rewardId(skill.getId())
                .name(skill.getName())
                .description(skill.getDescription())
                .build();
    }

    @Override
    public void validateRewardId(UUID rewardId) {
        if (!skillRepository.existsById(rewardId)) {
            throw new ResourceNotFoundException("Skill not found: " + rewardId);
        }
    }
}
