package com.dnd.app.service.homebrew;

import com.dnd.app.domain.Subclass;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.SubclassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SubclassContentValidator implements HomebrewContentValidator {

    private final SubclassRepository subclassRepository;

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
        Subclass subclass = subclassRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Подкласс не найден: " + contentId));
        return ContentSummaryDto.builder()
                .id(subclass.getId())
                .name(subclass.getName())
                .description(subclass.getDescription())
                .classId(subclass.getParentClass() != null ? subclass.getParentClass().getId() : null)
                .className(subclass.getParentClass() != null ? subclass.getParentClass().getName() : null)
                .build();
    }

    @Override
    public UUID getOwnerId(UUID contentId) {
        Subclass subclass = subclassRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Подкласс не найден: " + contentId));
        return subclass.getHomebrew() != null && subclass.getHomebrew().getAuthor() != null
                ? subclass.getHomebrew().getAuthor().getId()
                : null;
    }
}
