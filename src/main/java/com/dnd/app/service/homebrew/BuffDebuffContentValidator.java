package com.dnd.app.service.homebrew;

import com.dnd.app.domain.BuffDebuff;
import com.dnd.app.dto.response.ContentSummaryDto;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.BuffDebuffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Класс BuffDebuffContentValidator описывает сервис homebrew-логики, который проверяет и обслуживает пользовательский контент.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Component
@RequiredArgsConstructor
public class BuffDebuffContentValidator implements HomebrewContentValidator {

    private final BuffDebuffRepository buffDebuffRepository;

    /**
     * Возвращает результат операции "get supported type" в рамках бизнес-логики homebrew-контента.
     * @return результат выполнения бизнес-операции
     */
    @Override
    public String getSupportedType() {
        return "BUFF_DEBUFF";
    }

    /**
     * Проверяет корректность операции "validate exists" в рамках бизнес-логики homebrew-контента.
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     */
    @Override
    public void validateExists(UUID contentId) {
        if (!buffDebuffRepository.existsById(contentId)) {
            throw new ResourceNotFoundException("Бафф/дебафф не найден: " + contentId);
        }
    }

    /**
     * Выполняет операции "summarize" в рамках бизнес-логики homebrew-контента.
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Override
    public ContentSummaryDto summarize(UUID contentId) {
        BuffDebuff buffDebuff = buffDebuffRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Бафф/дебафф не найден: " + contentId));
        return ContentSummaryDto.builder()
                .id(buffDebuff.getId())
                .name(buffDebuff.getName())
                .description(buffDebuff.getDescription())
                .effectType(buffDebuff.getEffectType())
                .isBuff(buffDebuff.getIsBuff())
                .modifierValue(buffDebuff.getModifierValue())
                .durationRounds(buffDebuff.getDurationRounds())
                .targetStatId(buffDebuff.getTargetStat() != null ? buffDebuff.getTargetStat().getId() : null)
                .build();
    }

    /**
     * Возвращает результат операции "get owner id" в рамках бизнес-логики homebrew-контента.
     * @param contentId идентификатор content, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    @Override
    public UUID getOwnerId(UUID contentId) {
        BuffDebuff buffDebuff = buffDebuffRepository.findById(contentId)
                .orElseThrow(() -> new ResourceNotFoundException("Бафф/дебафф не найден: " + contentId));
        return buffDebuff.getHomebrew() != null && buffDebuff.getHomebrew().getAuthor() != null
                ? buffDebuff.getHomebrew().getAuthor().getId()
                : null;
    }
}
