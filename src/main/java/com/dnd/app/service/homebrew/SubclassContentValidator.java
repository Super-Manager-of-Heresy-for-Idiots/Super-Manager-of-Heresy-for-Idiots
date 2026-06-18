package com.dnd.app.service.homebrew;

import com.dnd.app.domain.content.ContentSubclass;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.ContentSubclassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SubclassContentValidator implements HomebrewContentValidator {

    private final ContentSubclassRepository subclassRepository;

    @Override
    public String getSupportedType() {
        return "SUBCLASS";
    }

    @Override
    public void validateExists(UUID contentId) {
        if (!subclassRepository.existsById(contentId)) {
            throw new ResourceNotFoundException("Подкласс не найден: " + contentId);
        }
    }

    @Override
    public ContentSummaryDto summarize(UUID contentId) {
        ContentSubclass subclass = subclassRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Подкласс не найден: " + contentId));
        return ContentSummaryDto.builder()
                .id(subclass.getId())
                .name(subclass.getNameRu())
                .description(null)
                .classId(subclass.getCharacterClass() != null ? subclass.getCharacterClass().getId() : null)
                .className(subclass.getCharacterClass() != null ? subclass.getCharacterClass().getNameRu() : null)
                .build();
    }

    @Override
    public UUID getOwnerId(UUID contentId) {
        ContentSubclass subclass = subclassRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Подкласс не найден: " + contentId));
        return subclass.getHomebrew() != null && subclass.getHomebrew().getAuthor() != null
                ? subclass.getHomebrew().getAuthor().getId()
                : null;
    }
}
