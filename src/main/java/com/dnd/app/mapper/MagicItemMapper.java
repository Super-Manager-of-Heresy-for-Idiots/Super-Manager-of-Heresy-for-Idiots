package com.dnd.app.mapper;

import com.dnd.app.domain.CurrencyType;
import com.dnd.app.domain.Rarity;
import com.dnd.app.domain.content.EquipmentItem;
import com.dnd.app.domain.content.MagicItem;
import com.dnd.app.domain.content.MagicItemAllowedEquipment;
import com.dnd.app.domain.content.MagicItemType;
import com.dnd.app.domain.content.MoneyValue;
import com.dnd.app.dto.content.ContentLabelDto;
import com.dnd.app.dto.content.MagicItemDetailResponse;
import com.dnd.app.util.Localization;
import org.springframework.stereotype.Component;

    /**
     * Преобразует данные операции "to detail" в рамках бизнес-логики преобразования данных.
     * @param m входящее значение m, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
/**
 * Класс MagicItemMapper описывает маппер, который преобразует доменные модели и DTO без изменения бизнес-правил.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Component
public class MagicItemMapper {

    /**
     * Преобразует данные операции "to detail" в рамках бизнес-логики преобразования данных.
     * @param m входящее значение m, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public MagicItemDetailResponse toDetail(MagicItem m, String lang) {
        return MagicItemDetailResponse.builder()
                .id(m.getId())
                .slug(m.getSlug())
                .name(Localization.pick(lang, m.getNameRu(), m.getNameEn(), fallback(m.getNameEn(), m.getNameRu())))
                .nameRu(m.getNameRu())
                .nameEn(m.getNameEn())
                .type(typeLabel(m.getType(), lang))
                .typeRestrictionRaw(m.getTypeRestrictionRaw())
                .rarity(rarityLabel(m.getRarity(), lang))
                .variableRarity(m.getVariableRarity())
                .attunementRequired(m.getAttunementRequired())
                .attunementRequirement(m.getAttunementRequirement())
                .cost(mapCost(m.getCost(), lang))
                .description(m.getDescription())
                .embeddedTablesDetected(m.getEmbeddedTablesDetected())
                .url(m.getUrl())
                .packageId(m.getHomebrew() != null ? m.getHomebrew().getId() : null)
                .allowedEquipment(m.getAllowedEquipment().stream().map(a -> mapAllowed(a, lang)).toList())
                .build();
    }

    private MagicItemDetailResponse.CostDto mapCost(MoneyValue m, String lang) {
        if (m == null) {
            return null;
        }
        return MagicItemDetailResponse.CostDto.builder()
                .amount(m.getAmount())
                .currency(currencyLabel(m.getCurrency(), lang))
                .copperValue(m.getCopperValue())
                .rawText(m.getRawText())
                .build();
    }

    private MagicItemDetailResponse.AllowedEquipmentDto mapAllowed(MagicItemAllowedEquipment a, String lang) {
        return MagicItemDetailResponse.AllowedEquipmentDto.builder()
                .equipment(equipmentLabel(a.getEquipmentItem(), lang))
                .rawText(a.getRawText())
                .build();
    }

    private ContentLabelDto typeLabel(MagicItemType t, String lang) {
        if (t == null) {
            return null;
        }
        return ContentLabelDto.builder()
                .id(t.getId())
                .slug(t.getSlug())
                .name(Localization.pick(lang, t.getNameRu(), t.getNameEn(), fallback(t.getNameEn(), t.getNameRu())))
                .nameRu(t.getNameRu())
                .nameEn(t.getNameEn())
                .build();
    }

    private ContentLabelDto rarityLabel(Rarity r, String lang) {
        if (r == null) {
            return null;
        }
        return ContentLabelDto.builder()
                .id(r.getId())
                .slug(r.getSlug())
                .name(Localization.pick(lang, r.getNameRu(), r.getNameEn(), fallback(r.getNameEn(), r.getNameRu())))
                .nameRu(r.getNameRu())
                .nameEn(r.getNameEn())
                .build();
    }

    private ContentLabelDto currencyLabel(CurrencyType c, String lang) {
        if (c == null) {
            return null;
        }
        return ContentLabelDto.builder()
                .id(c.getId())
                .slug(c.getSlug())
                .name(Localization.pick(lang, c.getNameRu(), c.getNameEn(), fallback(c.getNameEn(), c.getNameRu())))
                .nameRu(c.getNameRu())
                .nameEn(c.getNameEn())
                .build();
    }

    private ContentLabelDto equipmentLabel(EquipmentItem e, String lang) {
        if (e == null) {
            return null;
        }
        return ContentLabelDto.builder()
                .id(e.getId())
                .slug(e.getSlug())
                .name(Localization.pick(lang, e.getNameRu(), e.getNameEn(), fallback(e.getNameEn(), e.getNameRu())))
                .nameRu(e.getNameRu())
                .nameEn(e.getNameEn())
                .build();
    }

    private String fallback(String preferred, String other) {
        return preferred != null ? preferred : other;
    }
}
