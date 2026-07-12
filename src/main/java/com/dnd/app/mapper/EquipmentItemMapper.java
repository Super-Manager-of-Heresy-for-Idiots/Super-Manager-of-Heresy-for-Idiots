package com.dnd.app.mapper;

import com.dnd.app.domain.CurrencyType;
import com.dnd.app.domain.DamageType;
import com.dnd.app.domain.content.ArmorStat;
import com.dnd.app.domain.content.DiceFormula;
import com.dnd.app.domain.content.EquipmentCategory;
import com.dnd.app.domain.content.EquipmentItem;
import com.dnd.app.domain.content.MoneyValue;
import com.dnd.app.domain.content.WeaponItemProperty;
import com.dnd.app.domain.content.WeaponMastery;
import com.dnd.app.domain.content.WeaponProperty;
import com.dnd.app.domain.content.WeaponStat;
import com.dnd.app.dto.content.ContentLabelDto;
import com.dnd.app.dto.content.EquipmentItemDetailResponse;
import com.dnd.app.util.Localization;
import org.springframework.stereotype.Component;

    /**
     * Преобразует данные операции "to detail" в рамках бизнес-логики преобразования данных.
     * @param e входящее значение e, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
/**
 * Класс EquipmentItemMapper описывает маппер, который преобразует доменные модели и DTO без изменения бизнес-правил.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Component
public class EquipmentItemMapper {

    /**
     * Преобразует данные операции "to detail" в рамках бизнес-логики преобразования данных.
     * @param e входящее значение e, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public EquipmentItemDetailResponse toDetail(EquipmentItem e, String lang) {
        return EquipmentItemDetailResponse.builder()
                .id(e.getId())
                .slug(e.getSlug())
                .name(Localization.pick(lang, e.getNameRu(), e.getNameEn(), fallback(e.getNameEn(), e.getNameRu())))
                .nameRu(e.getNameRu())
                .nameEn(e.getNameEn())
                .kind(e.getKind())
                .category(categoryLabel(e.getCategory(), lang))
                .cost(mapCost(e.getCost(), lang))
                .weightLb(e.getWeightLb())
                .propertiesText(e.getPropertiesText())
                .url(e.getUrl())
                .packageId(com.dnd.app.util.HomebrewOrigin.id(e.getHomebrew()))
                .source(com.dnd.app.util.HomebrewOrigin.source(e.getHomebrew()))
                .homebrewTitle(com.dnd.app.util.HomebrewOrigin.title(e.getHomebrew()))
                .weaponStat(mapWeaponStat(e.getWeaponStat(), lang))
                .armorStat(mapArmorStat(e.getArmorStat()))
                .weaponProperties(e.getWeaponProperties().stream().map(p -> mapWeaponProperty(p, lang)).toList())
                .build();
    }

    private EquipmentItemDetailResponse.CostDto mapCost(MoneyValue m, String lang) {
        if (m == null) {
            return null;
        }
        return EquipmentItemDetailResponse.CostDto.builder()
                .amount(m.getAmount())
                .currency(currencyLabel(m.getCurrency(), lang))
                .copperValue(m.getCopperValue())
                .rawText(m.getRawText())
                .build();
    }

    private EquipmentItemDetailResponse.WeaponStatDto mapWeaponStat(WeaponStat s, String lang) {
        if (s == null) {
            return null;
        }
        return EquipmentItemDetailResponse.WeaponStatDto.builder()
                .damageDice(mapDice(s.getDamageDiceFormula()))
                .damageType(damageTypeLabel(s.getDamageType(), lang))
                .flatDamage(s.getFlatDamage())
                .mastery(masteryLabel(s.getMastery(), lang))
                .build();
    }

    private EquipmentItemDetailResponse.ArmorStatDto mapArmorStat(ArmorStat a) {
        if (a == null) {
            return null;
        }
        return EquipmentItemDetailResponse.ArmorStatDto.builder()
                .baseAc(a.getBaseAc())
                .dexBonusAllowed(a.getDexBonusAllowed())
                .maxDexBonus(a.getMaxDexBonus())
                .strengthRequired(a.getStrengthRequired())
                .stealthDisadvantage(a.getStealthDisadvantage())
                .armorClassRaw(a.getArmorClassRaw())
                .build();
    }

    private EquipmentItemDetailResponse.WeaponPropertyDto mapWeaponProperty(WeaponItemProperty p, String lang) {
        return EquipmentItemDetailResponse.WeaponPropertyDto.builder()
                .property(weaponPropertyLabel(p.getWeaponProperty(), lang))
                .normalRangeFt(p.getNormalRangeFt())
                .longRangeFt(p.getLongRangeFt())
                .versatileDice(mapDice(p.getVersatileDiceFormula()))
                .ammunitionEquipmentItemId(p.getAmmunitionEquipmentItemId())
                .rawText(p.getRawText())
                .build();
    }

    private EquipmentItemDetailResponse.DiceDto mapDice(DiceFormula d) {
        if (d == null) {
            return null;
        }
        return EquipmentItemDetailResponse.DiceDto.builder()
                .diceCount(d.getDiceCount())
                .dieSize(d.getDieSize())
                .bonus(d.getBonus())
                .rawText(d.getRawText())
                .build();
    }

    private ContentLabelDto categoryLabel(EquipmentCategory c, String lang) {
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

    private ContentLabelDto damageTypeLabel(DamageType d, String lang) {
        if (d == null) {
            return null;
        }
        return ContentLabelDto.builder()
                .id(d.getId())
                .slug(d.getSlug())
                .name(Localization.pick(lang, d.getNameRu(), d.getNameEn(), fallback(d.getNameEn(), d.getNameRu())))
                .nameRu(d.getNameRu())
                .nameEn(d.getNameEn())
                .build();
    }

    private ContentLabelDto masteryLabel(WeaponMastery m, String lang) {
        if (m == null) {
            return null;
        }
        return ContentLabelDto.builder()
                .id(m.getId())
                .slug(m.getSlug())
                .name(Localization.pick(lang, m.getNameRu(), m.getNameEn(), fallback(m.getNameEn(), m.getNameRu())))
                .nameRu(m.getNameRu())
                .nameEn(m.getNameEn())
                .build();
    }

    private ContentLabelDto weaponPropertyLabel(WeaponProperty w, String lang) {
        if (w == null) {
            return null;
        }
        return ContentLabelDto.builder()
                .id(w.getId())
                .slug(w.getSlug())
                .name(Localization.pick(lang, w.getNameRu(), w.getNameEn(), fallback(w.getNameEn(), w.getNameRu())))
                .nameRu(w.getNameRu())
                .nameEn(w.getNameEn())
                .build();
    }

    private String fallback(String preferred, String other) {
        return preferred != null ? preferred : other;
    }
}
