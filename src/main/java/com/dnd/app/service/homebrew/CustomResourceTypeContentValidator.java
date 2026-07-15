package com.dnd.app.service.homebrew;

import com.dnd.app.domain.CustomResourceType;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.CustomResourceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Валидатор контента типа CUSTOM_RESOURCE (P2-3). Обеспечивает существование, сводку и владельца пользовательского
 * ресурса (custom_resource_types — тот же механизм, что Ярость/Ки) при регистрации в homebrew-пакете.
 * Владелец берётся из автора пакета-родителя.
 */
@Component
@RequiredArgsConstructor
public class CustomResourceTypeContentValidator implements HomebrewContentValidator {

    private final CustomResourceTypeRepository customResourceTypeRepository;

    @Override
    public String getSupportedType() {
        return "CUSTOM_RESOURCE";
    }

    @Override
    public void validateExists(UUID contentId) {
        if (!customResourceTypeRepository.existsById(contentId)) {
            throw new ResourceNotFoundException("Ресурс не найден: " + contentId);
        }
    }

    @Override
    public ContentSummaryDto summarize(UUID contentId) {
        CustomResourceType res = require(contentId);
        return ContentSummaryDto.builder()
                .id(res.getId())
                .name(res.getName())
                .description(res.getDescription())
                .classId(res.getClassBound() != null ? res.getClassBound().getId() : null)
                .className(res.getClassBound() != null ? res.getClassBound().getNameRu() : null)
                .build();
    }

    @Override
    public UUID getOwnerId(UUID contentId) {
        CustomResourceType res = require(contentId);
        return res.getHomebrew() != null ? res.getHomebrew().getAuthor().getId() : null;
    }

    private CustomResourceType require(UUID contentId) {
        return customResourceTypeRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Ресурс не найден: " + contentId));
    }
}
