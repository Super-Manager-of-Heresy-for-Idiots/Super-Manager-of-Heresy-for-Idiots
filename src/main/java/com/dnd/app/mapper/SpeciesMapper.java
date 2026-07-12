package com.dnd.app.mapper;

import com.dnd.app.domain.CreatureSize;
import com.dnd.app.domain.DamageType;
import com.dnd.app.domain.Spell;
import com.dnd.app.domain.content.ContentCreatureType;
import com.dnd.app.domain.content.Species;
import com.dnd.app.domain.content.SpeciesSpeed;
import com.dnd.app.domain.content.SpeciesTrait;
import com.dnd.app.domain.content.SpeciesTraitEffect;
import com.dnd.app.dto.content.ContentLabelDto;
import com.dnd.app.dto.content.SpeciesDetailResponse;
import com.dnd.app.util.Localization;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

    /**
     * Преобразует данные операции "to detail" в рамках бизнес-логики преобразования данных.
     * @param s входящее значение s, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
/**
 * Класс SpeciesMapper описывает маппер, который преобразует доменные модели и DTO без изменения бизнес-правил.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Component
public class SpeciesMapper {

    /**
     * Преобразует данные операции "to detail" в рамках бизнес-логики преобразования данных.
     * @param s входящее значение s, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public SpeciesDetailResponse toDetail(Species s, String lang) {
        return SpeciesDetailResponse.builder()
                .id(s.getId())
                .slug(s.getSlug())
                .name(Localization.pick(lang, s.getNameRu(), s.getNameEn(), fallback(s.getNameEn(), s.getNameRu())))
                .nameRu(s.getNameRu())
                .nameEn(s.getNameEn())
                .description(s.getDescription())
                .packageId(com.dnd.app.util.HomebrewOrigin.id(s.getHomebrew()))
                .source(com.dnd.app.util.HomebrewOrigin.source(s.getHomebrew()))
                .homebrewTitle(com.dnd.app.util.HomebrewOrigin.title(s.getHomebrew()))
                .creatureType(creatureTypeLabel(s.getCreatureType(), lang))
                .sizeOptions(s.getSizeOptions().stream()
                        .map(sz -> sizeLabel(sz, lang))
                        .sorted(Comparator.comparing(ContentLabelDto::getSlug,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList())
                .speeds(s.getSpeeds().stream().map(this::mapSpeed).toList())
                .traits(s.getTraits().stream().map(t -> mapTrait(t, lang)).toList())
                .build();
    }

    private SpeciesDetailResponse.SpeedDto mapSpeed(SpeciesSpeed sp) {
        return SpeciesDetailResponse.SpeedDto.builder()
                .type(sp.getSpeedTypeSlug())
                .amountFt(sp.getAmountFt())
                .rawText(sp.getRawText())
                .build();
    }

    private SpeciesDetailResponse.TraitDto mapTrait(SpeciesTrait t, String lang) {
        return SpeciesDetailResponse.TraitDto.builder()
                .slug(t.getSlug())
                .name(t.getName())
                .description(t.getDescription())
                .effects(t.getEffects().stream().map(e -> mapEffect(e, lang)).toList())
                .build();
    }

    private SpeciesDetailResponse.EffectDto mapEffect(SpeciesTraitEffect e, String lang) {
        return SpeciesDetailResponse.EffectDto.builder()
                .effectType(e.getEffectType())
                .damageType(damageTypeLabel(e.getDamageType(), lang))
                .spell(spellLabel(e.getSpell(), lang))
                .rangeFt(e.getRangeFt())
                .build();
    }

    // --- label helpers ---

    private ContentLabelDto creatureTypeLabel(ContentCreatureType ct, String lang) {
        if (ct == null) {
            return null;
        }
        return ContentLabelDto.builder()
                .id(ct.getId())
                .slug(ct.getSlug())
                .name(Localization.pick(lang, ct.getNameRu(), ct.getNameEn(), fallback(ct.getNameEn(), ct.getNameRu())))
                .nameRu(ct.getNameRu())
                .nameEn(ct.getNameEn())
                .build();
    }

    private ContentLabelDto sizeLabel(CreatureSize sz, String lang) {
        return ContentLabelDto.builder()
                .id(sz.getId())
                .slug(sz.getSlug())
                .name(Localization.pick(lang, sz.getNameRu(), sz.getNameEn(), fallback(sz.getNameEn(), sz.getNameRu())))
                .nameRu(sz.getNameRu())
                .nameEn(sz.getNameEn())
                .build();
    }

    private ContentLabelDto damageTypeLabel(DamageType dt, String lang) {
        if (dt == null) {
            return null;
        }
        return ContentLabelDto.builder()
                .id(dt.getId())
                .slug(dt.getSlug())
                .name(Localization.pick(lang, dt.getNameRu(), dt.getNameEn(), fallback(dt.getNameEn(), dt.getNameRu())))
                .nameRu(dt.getNameRu())
                .nameEn(dt.getNameEn())
                .build();
    }

    private ContentLabelDto spellLabel(Spell sp, String lang) {
        if (sp == null) {
            return null;
        }
        return ContentLabelDto.builder()
                .id(sp.getId())
                .slug(sp.getSlug())
                .name(Localization.pick(lang, sp.getNameRu(), sp.getNameEn(), fallback(sp.getNameEn(), sp.getNameRu())))
                .nameRu(sp.getNameRu())
                .nameEn(sp.getNameEn())
                .build();
    }

    private String fallback(String preferred, String other) {
        return preferred != null ? preferred : other;
    }
}
