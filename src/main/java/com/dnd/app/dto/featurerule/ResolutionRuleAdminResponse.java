package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс ResolutionRuleAdminResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolutionRuleAdminResponse {
    private UUID id;
    private String resolutionType;
    private UUID abilityId;
    private UUID skillId;
    private String dcFormula;
    private String dcFormulaStatus;
    private String dcFormulaMessage;
}
