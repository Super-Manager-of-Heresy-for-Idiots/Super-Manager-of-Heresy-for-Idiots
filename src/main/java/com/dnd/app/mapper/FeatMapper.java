package com.dnd.app.mapper;

import com.dnd.app.domain.Feat;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.content.FeatCategory;
import com.dnd.app.domain.content.FeatPrerequisite;
import com.dnd.app.domain.content.FeatSection;
import com.dnd.app.dto.content.ContentLabelDto;
import com.dnd.app.dto.content.FeatDetailResponse;
import com.dnd.app.util.Localization;
import org.springframework.stereotype.Component;

    /**
     * Преобразует данные операции "to detail" в рамках бизнес-логики преобразования данных.
     * @param f входящее значение f, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
/**
 * Класс FeatMapper описывает маппер, который преобразует доменные модели и DTO без изменения бизнес-правил.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Component
public class FeatMapper {

    /**
     * Преобразует данные операции "to detail" в рамках бизнес-логики преобразования данных.
     * @param f входящее значение f, используемое бизнес-сценарием
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public FeatDetailResponse toDetail(Feat f, String lang) {
        return FeatDetailResponse.builder()
                .id(f.getId())
                .slug(f.getSlug())
                .name(Localization.pick(lang, f.getNameRu(), f.getNameEn(), fallback(f.getNameEn(), f.getNameRu())))
                .nameRu(f.getNameRu())
                .nameEn(f.getNameEn())
                .description(f.getDescription())
                .repeatable(f.getRepeatable())
                .packageId(f.getHomebrew() != null ? f.getHomebrew().getId() : null)
                .category(categoryLabel(f.getCategory(), lang))
                .prerequisites(f.getPrerequisites().stream().map(p -> mapPrerequisite(p, lang)).toList())
                .sections(f.getSections().stream().map(this::mapSection).toList())
                .build();
    }

    private FeatDetailResponse.PrerequisiteDto mapPrerequisite(FeatPrerequisite p, String lang) {
        return FeatDetailResponse.PrerequisiteDto.builder()
                .type(p.getPrerequisiteType())
                .levelRequired(p.getLevelRequired())
                .abilityScore(abilityLabel(p.getAbilityScore(), lang))
                .minimumScore(p.getMinimumScore())
                .groupKey(p.getGroupKey())
                .rawText(p.getRawText())
                .build();
    }

    private FeatDetailResponse.SectionDto mapSection(FeatSection s) {
        return FeatDetailResponse.SectionDto.builder()
                .title(s.getTitle())
                .body(s.getBody())
                .build();
    }

    private ContentLabelDto categoryLabel(FeatCategory c, String lang) {
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

    private ContentLabelDto abilityLabel(StatType a, String lang) {
        if (a == null) {
            return null;
        }
        return ContentLabelDto.builder()
                .id(a.getId())
                .slug(a.getSlug())
                .name(Localization.pick(lang, a.getNameRu(), a.getNameEn(), fallback(a.getNameEn(), a.getNameRu())))
                .nameRu(a.getNameRu())
                .nameEn(a.getNameEn())
                .build();
    }

    private String fallback(String preferred, String other) {
        return preferred != null ? preferred : other;
    }
}
