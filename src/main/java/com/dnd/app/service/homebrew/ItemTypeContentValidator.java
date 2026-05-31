package com.dnd.app.service.homebrew;

import com.dnd.app.domain.ItemType;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.ItemTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ItemTypeContentValidator implements HomebrewContentValidator {

    private final ItemTypeRepository itemTypeRepository;

    @Override
    public String getSupportedType() {
        return "ITEM_TYPE";
    }

    @Override
    public void validateExists(UUID contentId) {
        if (!itemTypeRepository.existsById(contentId)) {
            throw new ResourceNotFoundException("Тип предмета не найден: " + contentId);
        }
    }

    @Override
    public ContentSummaryDto summarize(UUID contentId) {
        ItemType item = itemTypeRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Тип предмета не найден: " + contentId));
        return ContentSummaryDto.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .slot(item.getSlot().name())
                .build();
    }

    @Override
    public UUID getOwnerId(UUID contentId) {
        ItemType item = itemTypeRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Тип предмета не найден: " + contentId));
        return item.getHomebrew() != null ? item.getHomebrew().getAuthor().getId() : null;
    }
}
