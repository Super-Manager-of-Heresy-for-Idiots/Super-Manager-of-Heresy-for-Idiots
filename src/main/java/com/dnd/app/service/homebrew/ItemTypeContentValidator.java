package com.dnd.app.service.homebrew;

import com.dnd.app.domain.ItemType;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.ItemTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс ItemTypeContentValidator описывает сервис homebrew-логики, который проверяет и обслуживает пользовательский контент.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Component
@RequiredArgsConstructor
public class ItemTypeContentValidator implements HomebrewContentValidator {

    private final ItemTypeRepository itemTypeRepository;

    /**
     * Возвращает результат операции "get supported type" в рамках бизнес-логики homebrew-контента.
     * @return результат выполнения бизнес-операции
     */
    @Override
    public String getSupportedType() {
        return "ITEM_TYPE";
    }

    /**
     * Проверяет корректность операции "validate exists" в рамках бизнес-логики homebrew-контента.
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     */
    @Override
    public void validateExists(UUID contentId) {
        if (!itemTypeRepository.existsById(contentId)) {
            throw new ResourceNotFoundException("Тип предмета не найден: " + contentId);
        }
    }

    /**
     * Выполняет операции "summarize" в рамках бизнес-логики homebrew-контента.
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Override
    public ContentSummaryDto summarize(UUID contentId) {
        ItemType item = itemTypeRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Тип предмета не найден: " + contentId));
        return ContentSummaryDto.builder()
                .id(item.getId())
                .name(item.getName())
                .description(item.getDescription())
                .slot(item.getSlot().getCode())
                .build();
    }

    /**
     * Возвращает результат операции "get owner id" в рамках бизнес-логики homebrew-контента.
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Override
    public UUID getOwnerId(UUID contentId) {
        ItemType item = itemTypeRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Тип предмета не найден: " + contentId));
        return item.getHomebrew() != null ? item.getHomebrew().getAuthor().getId() : null;
    }
}
