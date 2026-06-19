package com.dnd.app.service.homebrew;

import com.dnd.app.domain.content.Species;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Homebrew-package reference validator for the new content-model {@link Species}
 * (D&D 2024 race replacement). Mirrors {@code CharacterClassContentValidator};
 * the legacy {@code RaceContentValidator} ("RACE") is removed in S5.
 */
@Component
@RequiredArgsConstructor
public class SpeciesContentValidator implements HomebrewContentValidator {

    private final SpeciesRepository speciesRepository;

    @Override
    public String getSupportedType() {
        return "SPECIES";
    }

    @Override
    public void validateExists(UUID contentId) {
        if (!speciesRepository.existsById(contentId)) {
            throw new ResourceNotFoundException("Species not found: " + contentId);
        }
    }

    @Override
    public ContentSummaryDto summarize(UUID contentId) {
        Species species = speciesRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Species not found: " + contentId));
        return ContentSummaryDto.builder()
                .id(species.getId())
                .name(species.getNameEn() != null ? species.getNameEn() : species.getNameRu())
                .description(species.getDescription())
                .build();
    }

    @Override
    public UUID getOwnerId(UUID contentId) {
        Species species = speciesRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Species not found: " + contentId));
        return species.getHomebrew() != null ? species.getHomebrew().getAuthor().getId() : null;
    }
}
