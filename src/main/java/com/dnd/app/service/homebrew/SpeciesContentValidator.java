package com.dnd.app.service.homebrew;

import com.dnd.app.domain.content.Species;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс SpeciesContentValidator описывает сервис homebrew-логики, который проверяет и обслуживает пользовательский контент.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Component
@RequiredArgsConstructor
public class SpeciesContentValidator implements HomebrewContentValidator {

    private final SpeciesRepository speciesRepository;

    /**
     * Возвращает результат операции "get supported type" в рамках бизнес-логики homebrew-контента.
     * @return результат выполнения бизнес-операции
     */
    @Override
    public String getSupportedType() {
        return "SPECIES";
    }

    /**
     * Проверяет корректность операции "validate exists" в рамках бизнес-логики homebrew-контента.
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     */
    @Override
    public void validateExists(UUID contentId) {
        if (!speciesRepository.existsById(contentId)) {
            throw new ResourceNotFoundException("Species not found: " + contentId);
        }
    }

    /**
     * Выполняет операции "summarize" в рамках бизнес-логики homebrew-контента.
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Возвращает результат операции "get owner id" в рамках бизнес-логики homebrew-контента.
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Override
    public UUID getOwnerId(UUID contentId) {
        Species species = speciesRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Species not found: " + contentId));
        return species.getHomebrew() != null ? species.getHomebrew().getAuthor().getId() : null;
    }
}
