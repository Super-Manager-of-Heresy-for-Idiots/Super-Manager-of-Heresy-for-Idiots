package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс ContentSummaryDto описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContentSummaryDto {

    private UUID id;
    private String name;
    private String description;
    private String slot;
    private String skillType;
    private String prerequisites;
    private String effectType;
    private Boolean isBuff;
    // Поля баффа/дебаффа для lossless-префилла при правке (P1-6).
    private Integer modifierValue;
    private Integer durationRounds;
    private UUID targetStatId;
    private UUID classId;
    private String className;
}
