package com.dnd.app.service.homebrew;

import com.dnd.app.domain.content.ContentSubclass;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.ContentSubclassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс SubclassContentValidator описывает сервис homebrew-логики, который проверяет и обслуживает пользовательский контент.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Component
@RequiredArgsConstructor
public class SubclassContentValidator implements HomebrewContentValidator {

    private final ContentSubclassRepository subclassRepository;

    /**
     * Возвращает результат операции "get supported type" в рамках бизнес-логики homebrew-контента.
     * @return результат выполнения бизнес-операции
     */
    @Override
    public String getSupportedType() {
        return "SUBCLASS";
    }

    /**
     * Проверяет корректность операции "validate exists" в рамках бизнес-логики homebrew-контента.
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     */
    @Override
    public void validateExists(UUID contentId) {
        if (!subclassRepository.existsById(contentId)) {
            throw new ResourceNotFoundException("Подкласс не найден: " + contentId);
        }
    }

    /**
     * Выполняет операции "summarize" в рамках бизнес-логики homebrew-контента.
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Возвращает результат операции "get owner id" в рамках бизнес-логики homebrew-контента.
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Override
    public UUID getOwnerId(UUID contentId) {
        ContentSubclass subclass = subclassRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Подкласс не найден: " + contentId));
        return subclass.getHomebrew() != null && subclass.getHomebrew().getAuthor() != null
                ? subclass.getHomebrew().getAuthor().getId()
                : null;
    }
}
