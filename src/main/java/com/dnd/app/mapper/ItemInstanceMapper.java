package com.dnd.app.mapper;

import com.dnd.app.domain.ItemInstance;
import com.dnd.app.domain.ItemTemplate;
import com.dnd.app.domain.content.DiceFormula;
import com.dnd.app.domain.content.EquipmentItem;
import com.dnd.app.domain.content.MagicItem;
import com.dnd.app.domain.content.MoneyValue;
import com.dnd.app.domain.content.WeaponStat;
import com.dnd.app.dto.response.ItemInstanceResponse;

import java.math.BigDecimal;

/**
 * Класс ItemInstanceMapper описывает маппер, который преобразует доменные модели и DTO без изменения бизнес-правил.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public final class ItemInstanceMapper {

    private ItemInstanceMapper() {
    }

    /**
     * Преобразует данные операции "to response" в рамках бизнес-логики преобразования данных.
     * @param instance входящее значение instance, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static ItemInstanceResponse toResponse(ItemInstance instance) {
        String rarity = null;
        String itemTypeName = null;
        String damageDice = null;
        String damageType = null;
        boolean attunementRequired = false;
        BigDecimal priceGold = null;

        if (instance.getTemplate() != null) {
            ItemTemplate t = instance.getTemplate();
            rarity = t.getRarity() != null ? t.getRarity().getSlug() : null;
            itemTypeName = t.getItemType() != null ? t.getItemType().getName() : null;
            damageDice = t.getDamageDice();
            damageType = t.getDamageType() != null ? t.getDamageType().getSlug() : null;
            attunementRequired = Boolean.TRUE.equals(t.getAttunementRequired());
            priceGold = t.getPriceGold();
        } else if (instance.getEquipmentItem() != null) {
            EquipmentItem e = instance.getEquipmentItem();
            itemTypeName = e.getCategory() != null
                    ? ruFirst(e.getCategory().getNameRu(), e.getCategory().getNameEn())
                    : e.getKind();
            WeaponStat ws = e.getWeaponStat();
            if (ws != null) {
                damageDice = formatDice(ws.getDamageDiceFormula());
                damageType = ws.getDamageType() != null ? ws.getDamageType().getSlug() : null;
            }
            priceGold = goldFromCost(e.getCost());
            // mundane equipment carries no rarity
        } else if (instance.getMagicItem() != null) {
            MagicItem m = instance.getMagicItem();
            rarity = m.getRarity() != null ? m.getRarity().getSlug() : null;
            itemTypeName = m.getType() != null
                    ? ruFirst(m.getType().getNameRu(), m.getType().getNameEn())
                    : null;
            attunementRequired = Boolean.TRUE.equals(m.getAttunementRequired());
            priceGold = goldFromCost(m.getCost());
        }

        return ItemInstanceResponse.builder()
                .id(instance.getId())
                .templateId(instance.getReferenceId())
                .templateName(instance.getBaseName())
                .displayName(instance.getDisplayName())
                .customName(instance.getCustomName())
                .quantity(instance.getQuantity())
                .isUnique(instance.getIsUnique())
                .slot(instance.getSlot() != null ? instance.getSlot().getCode() : null)
                .notes(instance.getNotes())
                .rarity(rarity)
                .itemTypeName(itemTypeName)
                .damageDice(damageDice)
                .damageType(damageType)
                .attuned(Boolean.TRUE.equals(instance.getAttuned()))
                .attunementRequired(attunementRequired)
                .priceGold(priceGold)
                .build();
    }

    private static String ruFirst(String ru, String en) {
        return ru != null && !ru.isBlank() ? ru : en;
    }

    /**
     * Converts a content-model {@link MoneyValue} (stored in copper) to gold. Content items
     * price everything through their {@code copper_value}; 100 copper = 1 gold, matching the
     * frontend {@code goldFromCopper} helper. Returns null when no price is known.
     */
    private static BigDecimal goldFromCost(MoneyValue cost) {
        if (cost == null || cost.getCopperValue() == null) {
            return null;
        }
        return cost.getCopperValue().movePointLeft(2);
    }

    /**
     * Выполняет операции "format dice" в рамках бизнес-логики преобразования данных.
     * @param dice входящее значение dice, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static String formatDice(DiceFormula dice) {
        if (dice == null) {
            return null;
        }
        if (dice.getRawText() != null && !dice.getRawText().isBlank()) {
            return dice.getRawText();
        }
        if (dice.getDieSize() == null) {
            return dice.getBonus() != null ? String.valueOf(dice.getBonus()) : null;
        }
        int count = dice.getDiceCount() != null ? dice.getDiceCount() : 1;
        StringBuilder sb = new StringBuilder().append(count).append('d').append(dice.getDieSize());
        if (dice.getBonus() != null && dice.getBonus() != 0) {
            sb.append(dice.getBonus() > 0 ? "+" : "").append(dice.getBonus());
        }
        return sb.toString();
    }
}
