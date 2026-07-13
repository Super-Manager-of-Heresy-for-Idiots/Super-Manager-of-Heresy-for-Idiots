package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Краткое описание умения, которое предмет даёт через feature-rules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemAbilitySummary {
    private UUID ruleId;
    private String name;
    private String actionType;
    private Charges charges;
    private boolean consumesItem;
    private boolean requiresAttunement;
    private boolean requiresEquipped;
    private boolean available;
    private String unavailableReason;

    /**
     * Состояние зарядов item-scoped ресурса.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Charges {
        private Integer current;
        private Integer max;
    }
}
