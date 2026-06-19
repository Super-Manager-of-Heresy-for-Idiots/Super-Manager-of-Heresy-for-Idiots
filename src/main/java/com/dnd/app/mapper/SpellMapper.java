package com.dnd.app.mapper;

import com.dnd.app.domain.Spell;
import com.dnd.app.domain.SpellSchool;
import com.dnd.app.domain.content.ContentCharacterClass;
import com.dnd.app.domain.content.ContentSubclass;
import com.dnd.app.domain.content.SpellComponent;
import com.dnd.app.dto.content.ContentLabelDto;
import com.dnd.app.dto.content.SpellDetailResponse;
import com.dnd.app.util.Localization;
import org.springframework.stereotype.Component;

import java.util.Comparator;

/**
 * Maps the new-content spell graph ({@link Spell} + school + components + class/subclass
 * availability) to {@link SpellDetailResponse}. Must run inside a read-only transaction:
 * lazy collections are resolved on demand.
 */
@Component
public class SpellMapper {

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
                .description(s.getDescription())
                .higherLevels(s.getHigherLevels())
                .packageId(s.getHomebrew() != null ? s.getHomebrew().getId() : null)
                .components(s.getComponents().stream().map(this::mapComponent).toList())
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
                .build();
    }

    private SpellDetailResponse.ComponentDto mapComponent(SpellComponent c) {
        return SpellDetailResponse.ComponentDto.builder()
                .component(c.getComponentSlug())
                .materialText(c.getMaterialText())
                .consumed(c.getConsumed())
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
