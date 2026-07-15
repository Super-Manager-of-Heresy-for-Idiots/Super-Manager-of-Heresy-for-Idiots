package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Отчёт покрытия корпуса предметов ({@code magic_item}) исполняемыми правилами feature-rules.
 * Используется вкладкой «Предметы» в Rule Workbench (ITEM_ABIL Фаза 4, §5.1).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemRuleCoverageReport {
    /** Всего магических предметов в корпусе. */
    private int totalItems;
    /** Предметов, у которых есть хотя бы одно item-правило. */
    private int itemsWithRules;
    /** Предметов, у которых есть хотя бы одно approved item-правило. */
    private int itemsWithApprovedRules;
    /** Предметов без единого правила. */
    private int itemsWithoutRules;
    /** Всего item-правил (owner_type = ITEM_MAGIC). */
    private int totalRules;
    /** approved item-правил. */
    private long approvedRules;
    /** item-правил в статусе needs_review. */
    private long needsReviewRules;
    /** Разбивка item-правил по типу (rule_type -> count). */
    private Map<String, Long> rulesByType;
    /** Разбивка item-правил по статусу ревью (review_status -> count). */
    private Map<String, Long> rulesByStatus;
}
