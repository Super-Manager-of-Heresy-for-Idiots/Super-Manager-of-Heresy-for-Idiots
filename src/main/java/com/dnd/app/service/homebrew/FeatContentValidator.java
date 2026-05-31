package com.dnd.app.service.homebrew;

import com.dnd.app.domain.Feat;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.FeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FeatContentValidator implements HomebrewContentValidator {

    private final FeatRepository featRepository;

    @Override
    public String getSupportedType() {
        return "FEAT";
    }

    @Override
    public void validateExists(UUID contentId) {
        if (!featRepository.existsById(contentId)) {
            throw new ResourceNotFoundException("Черта не найдена: " + contentId);
        }
    }

    @Override
    public ContentSummaryDto summarize(UUID contentId) {
        Feat feat = featRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Черта не найдена: " + contentId));
        return ContentSummaryDto.builder()
                .id(feat.getId())
                .name(feat.getName())
                .description(feat.getDescription())
                .prerequisites(feat.getPrerequisites())
                .build();
    }

    @Override
    public UUID getOwnerId(UUID contentId) {
        Feat feat = featRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Черта не найдена: " + contentId));
        return feat.getHomebrew() != null ? feat.getHomebrew().getAuthor().getId() : null;
    }
}
