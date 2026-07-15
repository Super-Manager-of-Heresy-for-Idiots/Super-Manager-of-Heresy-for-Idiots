package com.dnd.app.service.homebrew;

import com.dnd.app.domain.Background;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.BackgroundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Валидатор контента типа BACKGROUND (P2-3). Обеспечивает существование, сводку и владельца предыстории
 * при регистрации/резолвинге в homebrew-пакете. Владелец берётся из автора пакета-родителя (как у прочих типов).
 */
@Component
@RequiredArgsConstructor
public class BackgroundContentValidator implements HomebrewContentValidator {

    private final BackgroundRepository backgroundRepository;

    @Override
    public String getSupportedType() {
        return "BACKGROUND";
    }

    @Override
    public void validateExists(UUID contentId) {
        if (!backgroundRepository.existsById(contentId)) {
            throw new ResourceNotFoundException("Предыстория не найдена: " + contentId);
        }
    }

    @Override
    public ContentSummaryDto summarize(UUID contentId) {
        Background bg = require(contentId);
        return ContentSummaryDto.builder()
                .id(bg.getId())
                .name(bg.getNameRu())
                .description(bg.getDescription())
                .build();
    }

    @Override
    public UUID getOwnerId(UUID contentId) {
        Background bg = require(contentId);
        return bg.getHomebrew() != null ? bg.getHomebrew().getAuthor().getId() : null;
    }

    private Background require(UUID contentId) {
        return backgroundRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Предыстория не найдена: " + contentId));
    }
}
