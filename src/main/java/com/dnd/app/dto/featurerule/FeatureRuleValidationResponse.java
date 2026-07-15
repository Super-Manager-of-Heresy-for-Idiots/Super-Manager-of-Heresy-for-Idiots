package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Класс FeatureRuleValidationResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureRuleValidationResponse {
    /** True if the rule currently passes validation and could be approved. */
    private boolean valid;
    /** Human-readable problems blocking validity (empty when valid). */
    private List<String> problems;
    /**
     * Незаблокирующие предупреждения (не влияют на {@code valid}) — например, требование
     * аттюнмента у предмета, который его не поддерживает, или character-скоуп ресурса
     * у item-правила. Отображаются в Workbench отдельно от ошибок (ITEM_ABIL Фаза 1, §2.3).
     */
    @lombok.Builder.Default
    private List<String> warnings = new java.util.ArrayList<>();
}
