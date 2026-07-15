package com.dnd.app.dto.featurerule;

import lombok.Data;

/**
 * DTO запроса правки привязки item-правила (ITEM_ABIL Фаза 4, admin Workbench). Инвариант: {@code consumeOnUse}
 * несовместим с {@code requiresEquipped} (проверяется в сервисе).
 */
@Data
public class FeatureItemBindingEditRequest {
    private Boolean requiresEquipped;
    private Boolean requiresAttunement;
    private Boolean consumeOnUse;
    private Integer consumeQuantity;
    private String allowedSlotCode;
}
