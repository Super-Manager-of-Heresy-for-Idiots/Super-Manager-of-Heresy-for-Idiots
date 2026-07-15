package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Карточка определения предмета в Rule Workbench: сам предмет + его item-правила (с issues).
 * Аналог {@code FeatureRuleDetailResponse} для класса, но для семейства владельцев ITEM_*
 * (ITEM_ABIL Фаза 4, §5.1).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemRuleDetailResponse {
    /** Код owner-типа предмета (ITEM_MAGIC / ITEM_TEMPLATE / ITEM_EQUIPMENT). */
    private String ownerType;
    /** Идентификатор ОПРЕДЕЛЕНИЯ предмета (owner_id правил). */
    private UUID ownerId;
    /** Отображаемое имя предмета (локализованное, best-effort). */
    private String name;
    /** Требует ли предмет аттюнмента (для magic/template; null для equipment). */
    private Boolean attunementRequired;
    /** Правила предмета вместе с их issues. */
    private List<FeatureRuleResponse> rules;
}
