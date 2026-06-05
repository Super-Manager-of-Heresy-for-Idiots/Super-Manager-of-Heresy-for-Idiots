package com.dnd.app.service.homebrew;

import com.dnd.app.domain.CharacterRace;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CharacterRaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RaceContentValidator implements HomebrewContentValidator {

    private final CharacterRaceRepository raceRepository;

    @Override
    public String getSupportedType() {
        return "RACE";
    }

    @Override
    public void validateExists(UUID contentId) {
        if (!raceRepository.existsById(contentId)) {
            throw new ResourceNotFoundException("Race not found: " + contentId);
        }
    }

    @Override
    public ContentSummaryDto summarize(UUID contentId) {
        CharacterRace race = raceRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Race not found: " + contentId));
        return ContentSummaryDto.builder()
                .id(race.getId())
                .name(race.getName())
                .description(race.getDescription())
                .build();
    }

    @Override
    public UUID getOwnerId(UUID contentId) {
        CharacterRace race = raceRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Race not found: " + contentId));
        return race.getHomebrew() != null ? race.getHomebrew().getAuthor().getId() : null;
    }
}
