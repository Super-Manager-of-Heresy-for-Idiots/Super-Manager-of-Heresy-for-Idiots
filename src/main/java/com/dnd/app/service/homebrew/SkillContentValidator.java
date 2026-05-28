package com.dnd.app.service.homebrew;

import com.dnd.app.domain.Skill;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SkillContentValidator implements HomebrewContentValidator {

    private final SkillRepository skillRepository;

    @Override
    public String getSupportedType() {
        return "SKILL";
    }

    @Override
    public void validateExists(UUID contentId) {
        if (!skillRepository.existsById(contentId)) {
            throw new ResourceNotFoundException("Skill not found: " + contentId);
        }
    }

    @Override
    public ContentSummaryDto summarize(UUID contentId) {
        Skill skill = skillRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + contentId));
        return ContentSummaryDto.builder()
                .id(skill.getId())
                .name(skill.getName())
                .description(skill.getDescription())
                .skillType(skill.getSkillType())
                .build();
    }

    @Override
    public UUID getOwnerId(UUID contentId) {
        Skill skill = skillRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found: " + contentId));
        return skill.getOwner() != null ? skill.getOwner().getId() : null;
    }
}
