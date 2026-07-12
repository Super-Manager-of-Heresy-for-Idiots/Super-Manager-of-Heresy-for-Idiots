package com.dnd.app.mapper;

import com.dnd.app.domain.DamageType;
import com.dnd.app.domain.Monster;
import com.dnd.app.domain.Spell;
import com.dnd.app.domain.SpellSchool;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.ContentSubclass;
import com.dnd.app.domain.content.SpellComponent;
import com.dnd.app.domain.content.SpellDamage;
import com.dnd.app.dto.content.ContentLabelDto;
import com.dnd.app.dto.content.SpellDetailResponse;
import com.dnd.app.util.Localization;
import org.springframework.stereotype.Component;

import java.util.Comparator;

    /**
     * Преобразует данные операции "to detail" в рамках бизнес-логики преобразования данных.
     * @param s входящее значение s, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
/**
 * Класс SpellMapper описывает маппер, который преобразует доменные модели и DTO без изменения бизнес-правил.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Component
public class SpellMapper {

    /**
     * Преобразует данные операции "to detail" в рамках бизнес-логики преобразования данных.
     * @param s входящее значение s, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public SpellDetailResponse toDetail(Spell s, String lang) {
        return SpellDetailResponse.builder()
                .id(s.getId())
                .slug(s.getSlug())
                .name(Localization.pick(lang, s.getNameRu(), s.getNameEn(), fallback(s.getNameEn(), s.getNameRu())))
                .nameRu(s.getNameRu())
                .nameEn(s.getNameEn())
                .level(s.getLevel())
                .school(schoolLabel(s.getSchool(), lang))
                .castingTimeRaw(s.getCastingTimeRaw())
                .castingActionSlug(s.getCastingActionSlug())
                .ritual(s.getRitual())
                .rangeType(s.getRangeType())
                .rangeDistance(s.getRangeDistance())
                .rangeUnit(s.getRangeUnit())
                .durationRaw(s.getDurationRaw())
                .durationType(s.getDurationType())
                .durationAmount(s.getDurationAmount())
                .durationUnit(s.getDurationUnit())
                .concentration(s.getConcentration())
                .saveAbility(s.getSaveAbility())
                .attackRoll(s.getAttackRoll())
                .checkAbility(s.getCheckAbility())
                .checkSkill(s.getCheckSkill())
                .description(s.getDescription())
                .higherLevels(s.getHigherLevels())
                .packageId(com.dnd.app.util.HomebrewOrigin.id(s.getHomebrew()))
                .source(com.dnd.app.util.HomebrewOrigin.source(s.getHomebrew()))
                .homebrewTitle(com.dnd.app.util.HomebrewOrigin.title(s.getHomebrew()))
                .warning(s.getWarning())
                .warningReason(s.getWarningReason())
                .components(s.getComponents().stream().map(this::mapComponent).toList())
                .damage(s.getDamages().stream().map(d -> mapDamage(d, lang)).toList())
                .healing(s.getHealings().stream().map(this::mapHealing).toList())
                .classes(s.getClasses().stream()
                        .map(c -> classLabel(c, lang))
                        .sorted(Comparator.comparing(ContentLabelDto::getSlug,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList())
                .subclasses(s.getSubclasses().stream()
                        .map(sc -> subclassLabel(sc, lang))
                        .sorted(Comparator.comparing(ContentLabelDto::getSlug,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList())
                .summonedMonsters(s.getSummonedMonsters().stream()
                        .map(m -> summonedMonster(m, lang))
                        .sorted(Comparator.comparing(SpellDetailResponse.SummonedMonsterDto::getSlug,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                        .toList())
                .build();
    }

    private SpellDetailResponse.SummonedMonsterDto summonedMonster(Monster m, String lang) {
        return SpellDetailResponse.SummonedMonsterDto.builder()
                .id(m.getId())
                .slug(m.getSlug())
                .name(Localization.pick(lang, m.getNameRusloc(), m.getNameEngloc(),
                        fallback(m.getNameRusloc(), m.getNameEngloc())))
                .nameRu(m.getNameRusloc())
                .nameEn(m.getNameEngloc())
                .crRating(m.getCrRating())
                .build();
    }

    private SpellDetailResponse.ComponentDto mapComponent(SpellComponent c) {
        return SpellDetailResponse.ComponentDto.builder()
                .component(c.getComponentSlug())
                .materialText(c.getMaterialText())
                .consumed(c.getConsumed())
                .build();
    }

    private SpellDetailResponse.DamageDto mapDamage(SpellDamage d, String lang) {
        return SpellDetailResponse.DamageDto.builder()
                .dice(d.getDice())
                .damageType(damageTypeLabel(d.getDamageType(), lang))
                .raw(d.getRaw())
                .build();
    }

    private SpellDetailResponse.HealingDto mapHealing(com.dnd.app.domain.content.SpellHealing h) {
        return SpellDetailResponse.HealingDto.builder()
                .dice(h.getDice())
                .flat(h.getFlat())
                .raw(h.getRaw())
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

    private ContentLabelDto schoolLabel(SpellSchool sc, String lang) {
        if (sc == null) {
            return null;
        }
        return ContentLabelDto.builder()
                .id(sc.getId())
                .slug(sc.getSlug())
                .name(Localization.pick(lang, sc.getNameRu(), sc.getNameEn(), fallback(sc.getNameEn(), sc.getNameRu())))
                .nameRu(sc.getNameRu())
                .nameEn(sc.getNameEn())
                .build();
    }

    private ContentLabelDto classLabel(ContentCharacterClass c, String lang) {
        return ContentLabelDto.builder()
                .id(c.getId())
                .slug(c.getSlug())
                .name(Localization.pick(lang, c.getNameRu(), c.getNameEn(), fallback(c.getNameEn(), c.getNameRu())))
                .nameRu(c.getNameRu())
                .nameEn(c.getNameEn())
                .build();
    }

    private ContentLabelDto subclassLabel(ContentSubclass sc, String lang) {
        return ContentLabelDto.builder()
                .id(sc.getId())
                .slug(sc.getSlug())
                .name(Localization.pick(lang, sc.getNameRu(), sc.getNameEn(), fallback(sc.getNameEn(), sc.getNameRu())))
                .nameRu(sc.getNameRu())
                .nameEn(sc.getNameEn())
                .build();
    }

    private String fallback(String preferred, String other) {
        return preferred != null ? preferred : other;
    }
}
