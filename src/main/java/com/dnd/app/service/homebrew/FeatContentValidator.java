package com.dnd.app.service.homebrew;

import com.dnd.app.domain.Feat;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.FeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс FeatContentValidator описывает сервис homebrew-логики, который проверяет и обслуживает пользовательский контент.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Component
@RequiredArgsConstructor
public class FeatContentValidator implements HomebrewContentValidator {

    private final FeatRepository featRepository;

    /**
     * Возвращает результат операции "get supported type" в рамках бизнес-логики homebrew-контента.
     * @return результат выполнения бизнес-операции
     */
    @Override
    public String getSupportedType() {
        return "FEAT";
    }

    /**
     * Проверяет корректность операции "validate exists" в рамках бизнес-логики homebrew-контента.
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     */
    @Override
    public void validateExists(UUID contentId) {
        if (!featRepository.existsById(contentId)) {
            throw new ResourceNotFoundException("Черта не найдена: " + contentId);
        }
    }

    /**
     * Выполняет операции "summarize" в рамках бизнес-логики homebrew-контента.
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Override
    public ContentSummaryDto summarize(UUID contentId) {
        Feat feat = featRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Черта не найдена: " + contentId));
        return ContentSummaryDto.builder()
                .id(feat.getId())
                .name(feat.getNameRu())
                .description(feat.getDescription())
                .build();
    }

    /**
     * Возвращает результат операции "get owner id" в рамках бизнес-логики homebrew-контента.
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Override
    public UUID getOwnerId(UUID contentId) {
        Feat feat = featRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Черта не найдена: " + contentId));
        return feat.getHomebrew() != null ? feat.getHomebrew().getAuthor().getId() : null;
    }
}
