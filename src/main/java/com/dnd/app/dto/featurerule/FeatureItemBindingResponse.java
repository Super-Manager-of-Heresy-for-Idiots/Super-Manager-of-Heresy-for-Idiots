package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO ответа с настройками привязки item-правила к предмету (ITEM_ABIL Фаза 4). Отражает {@code feature_item_binding}:
 * гейтинг умения предмета (экипировка / аттюнмент / расход / слот).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureItemBindingResponse {
    private UUID featureRuleId;
    private boolean requiresEquipped;
    private boolean requiresAttunement;
    private boolean consumeOnUse;
    private Integer consumeQuantity;
    private String allowedSlotCode;
}
