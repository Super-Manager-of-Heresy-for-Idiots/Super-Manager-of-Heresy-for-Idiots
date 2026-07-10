package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс CustomResourceTypeAdminResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomResourceTypeAdminResponse {
    private UUID id;
    private String name;
    private String description;
    private Integer maxValue;
    private String maxFormula;
    /** valid | invalid | null (advisory; the runtime falls back to maxValue if a formula errors). */
    private String maxFormulaStatus;
    private String maxFormulaMessage;
    private UUID classBoundId;
    private String className;
    private UUID featBoundId;
    private String featName;
    private String resetOn;
    private String shortRestRecovery;
    private String shortRestFormula;
    private String longRestRecovery;
    private String longRestFormula;
    private boolean homebrew;
}
