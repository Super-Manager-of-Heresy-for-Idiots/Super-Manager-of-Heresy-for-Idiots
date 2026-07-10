package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс ActionCostAdminResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionCostAdminResponse {
    private UUID id;
    private UUID actionTypeId;
    private Integer amount;
    private String conditionFormula;
    private String conditionFormulaStatus;
    private String conditionFormulaMessage;
}
