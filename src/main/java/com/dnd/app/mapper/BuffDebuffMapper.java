package com.dnd.app.mapper;

import com.dnd.app.domain.BuffDebuff;
import com.dnd.app.dto.response.BuffDebuffResponse;

/**
 * Класс BuffDebuffMapper описывает маппер, который преобразует доменные модели и DTO без изменения бизнес-правил.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public final class BuffDebuffMapper {

    private BuffDebuffMapper() {
    }

    /**
     * Преобразует данные операции "to response" в рамках бизнес-логики преобразования данных.
     * @param bd входящее значение bd, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static BuffDebuffResponse toResponse(BuffDebuff bd) {
        if (bd == null) {
            return null;
        }

        return BuffDebuffResponse.builder()
                .id(bd.getId())
                .name(bd.getName())
                .description(bd.getDescription())
                .effectType(bd.getEffectType())
                .targetStatId(bd.getTargetStat() != null ? bd.getTargetStat().getId() : null)
                .targetStatName(bd.getTargetStat() != null ? bd.getTargetStat().getNameRu() : null)
                .modifierValue(bd.getModifierValue())
                .durationRounds(bd.getDurationRounds())
                .isBuff(bd.getIsBuff())
                .createdAt(bd.getCreatedAt())
                .build();
    }
}
