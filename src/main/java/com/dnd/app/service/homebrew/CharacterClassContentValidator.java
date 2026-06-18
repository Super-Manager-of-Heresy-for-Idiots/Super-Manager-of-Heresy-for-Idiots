package com.dnd.app.service.homebrew;

import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.ContentCharacterClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CharacterClassContentValidator implements HomebrewContentValidator {

    private final ContentCharacterClassRepository characterClassRepository;

    @Override
    public String getSupportedType() {
        return "CHARACTER_CLASS";
    }

    @Override
    public void validateExists(UUID contentId) {
        if (!characterClassRepository.existsById(contentId)) {
            throw new ResourceNotFoundException("Класс персонажа не найден: " + contentId);
        }
    }

    @Override
    public ContentSummaryDto summarize(UUID contentId) {
        ContentCharacterClass cc = characterClassRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Класс персонажа не найден: " + contentId));
        return ContentSummaryDto.builder()
                .id(cc.getId())
                .name(cc.getNameRu())
                .description(cc.getSubtitle())
                .build();
    }

    @Override
    public UUID getOwnerId(UUID contentId) {
        ContentCharacterClass cc = characterClassRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Класс персонажа не найден: " + contentId));
        return cc.getHomebrew() != null ? cc.getHomebrew().getAuthor().getId() : null;
    }
}
