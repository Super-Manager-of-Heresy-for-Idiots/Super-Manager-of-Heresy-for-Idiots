package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс HealingRuleAdminResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealingRuleAdminResponse {
    private UUID id;
    private String amountFormula;
    /** Result type of the amount formula: {@code integer} (flat pool) or {@code dice} (rolled). */
    private String amountFormulaType;
    private String amountFormulaStatus;
    private String amountFormulaMessage;
    private UUID targetTypeId;
    private boolean tempHp;
    private boolean canReviveFromZero;
}
