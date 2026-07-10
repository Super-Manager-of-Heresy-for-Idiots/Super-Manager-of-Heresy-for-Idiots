package com.dnd.app.service.homebrew;

import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.ContentCharacterClassRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс CharacterClassContentValidator описывает сервис homebrew-логики, который проверяет и обслуживает пользовательский контент.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Component
@RequiredArgsConstructor
public class CharacterClassContentValidator implements HomebrewContentValidator {

    private final ContentCharacterClassRepository characterClassRepository;

    /**
     * Возвращает результат операции "get supported type" в рамках бизнес-логики homebrew-контента.
     * @return результат выполнения бизнес-операции
     */
    @Override
    public String getSupportedType() {
        return "CHARACTER_CLASS";
    }

    /**
     * Проверяет корректность операции "validate exists" в рамках бизнес-логики homebrew-контента.
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     */
    @Override
    public void validateExists(UUID contentId) {
        if (!characterClassRepository.existsById(contentId)) {
            throw new ResourceNotFoundException("Класс персонажа не найден: " + contentId);
        }
    }

    /**
     * Выполняет операции "summarize" в рамках бизнес-логики homebrew-контента.
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
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

    /**
     * Возвращает результат операции "get owner id" в рамках бизнес-логики homebrew-контента.
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Override
    public UUID getOwnerId(UUID contentId) {
        ContentCharacterClass cc = characterClassRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Класс персонажа не найден: " + contentId));
        return cc.getHomebrew() != null ? cc.getHomebrew().getAuthor().getId() : null;
    }
}
