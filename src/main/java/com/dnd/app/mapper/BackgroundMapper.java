package com.dnd.app.mapper;

import com.dnd.app.domain.Background;
import com.dnd.app.domain.Feat;
import com.dnd.app.domain.StatType;
import com.dnd.app.domain.content.BackgroundEquipmentChoiceGroup;
import com.dnd.app.domain.content.BackgroundEquipmentEntry;
import com.dnd.app.domain.content.BackgroundEquipmentOption;
import com.dnd.app.domain.content.BackgroundFeatOption;
import com.dnd.app.domain.content.BackgroundLanguageProficiency;
import com.dnd.app.domain.content.BackgroundToolProficiency;
import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.domain.content.FeatCategory;
import com.dnd.app.dto.content.BackgroundDetailResponse;
import com.dnd.app.dto.content.ContentLabelDto;
import com.dnd.app.util.Localization;
import org.springframework.stereotype.Component;

/**
 * Maps the new-content background graph ({@link Background} + ability/skill options,
 * feat/tool/language choices and equipment choice groups) to {@link BackgroundDetailResponse}.
 * Must run inside a read-only transaction: lazy collections are resolved on demand.
 */
@Component
public class BackgroundMapper {

    public BackgroundDetailResponse toDetail(Background b, String lang) {
        return BackgroundDetailResponse.builder()
                .id(b.getId())
                .slug(b.getSlug())
                .name(Localization.pick(lang, b.getNameRu(), b.getNameEn(), fallback(b.getNameEn(), b.getNameRu())))
                .nameRu(b.getNameRu())
                .nameEn(b.getNameEn())
                .description(b.getDescription())
                .url(b.getUrl())
                .packageId(b.getHomebrew() != null ? b.getHomebrew().getId() : null)
                .grantedFeat(featLabel(b.getGrantedFeat(), lang))
                .abilityOptions(b.getAbilityOptions().stream().map(a -> abilityLabel(a, lang)).toList())
                .skillProficiencies(b.getSkillProficiencies().stream().map(s -> skillLabel(s, lang)).toList())
                .featOptions(b.getFeatOptions().stream().map(o -> mapFeatOption(o, lang)).toList())
                .toolProficiencies(b.getToolProficiencies().stream().map(this::mapToolProficiency).toList())
                .languageProficiencies(b.getLanguageProficiencies().stream().map(this::mapLanguageProficiency).toList())
                .equipmentChoiceGroups(b.getEquipmentChoiceGroups().stream().map(this::mapEquipmentGroup).toList())
                .build();
    }

    private BackgroundDetailResponse.FeatOptionDto mapFeatOption(BackgroundFeatOption o, String lang) {
        return BackgroundDetailResponse.FeatOptionDto.builder()
                .feat(featLabel(o.getFeat(), lang))
                .featCategory(categoryLabel(o.getFeatCategory(), lang))
                .chooseCount(o.getChooseCount())
                .selectedOptionRaw(o.getSelectedOptionRaw())
                .recommendedFeat(featLabel(o.getRecommendedFeat(), lang))
                .rawText(o.getRawText())
                .build();
    }

    private BackgroundDetailResponse.ToolProficiencyDto mapToolProficiency(BackgroundToolProficiency t) {
        return BackgroundDetailResponse.ToolProficiencyDto.builder()
                .equipmentItemId(t.getEquipmentItemId())
                .chooseCount(t.getChooseCount())
                .choiceGroupSlug(t.getChoiceGroupSlug())
                .rawText(t.getRawText())
                .build();
    }

    private BackgroundDetailResponse.LanguageProficiencyDto mapLanguageProficiency(BackgroundLanguageProficiency l) {
        return BackgroundDetailResponse.LanguageProficiencyDto.builder()
                .languageSlug(l.getLanguageSlug())
                .chooseCount(l.getChooseCount())
                .rawText(l.getRawText())
                .build();
    }

    private BackgroundDetailResponse.EquipmentGroupDto mapEquipmentGroup(BackgroundEquipmentChoiceGroup g) {
        return BackgroundDetailResponse.EquipmentGroupDto.builder()
                .groupSlug(g.getGroupSlug())
                .chooseCount(g.getChooseCount())
                .rawText(g.getRawText())
                .options(g.getOptions().stream().map(this::mapEquipmentOption).toList())
                .build();
    }

    private BackgroundDetailResponse.EquipmentOptionDto mapEquipmentOption(BackgroundEquipmentOption o) {
        return BackgroundDetailResponse.EquipmentOptionDto.builder()
                .optionCode(o.getOptionCode())
                .sortOrder(o.getSortOrder())
                .rawText(o.getRawText())
                .entries(o.getEntries().stream().map(this::mapEquipmentEntry).toList())
                .build();
    }

    private BackgroundDetailResponse.EquipmentEntryDto mapEquipmentEntry(BackgroundEquipmentEntry e) {
        return BackgroundDetailResponse.EquipmentEntryDto.builder()
                .entryType(e.getEntryType())
                .equipmentItemId(e.getEquipmentItemId())
                .moneyValueId(e.getMoneyValueId())
                .quantity(e.getQuantity())
                .quantityUnitRaw(e.getQuantityUnitRaw())
                .variantNote(e.getVariantNote())
                .choiceRef(e.getChoiceRef())
                .rawText(e.getRawText())
                .build();
    }

    private ContentLabelDto featLabel(Feat f, String lang) {
        if (f == null) {
            return null;
        }
        return ContentLabelDto.builder()
                .id(f.getId())
                .slug(f.getSlug())
                .name(Localization.pick(lang, f.getNameRu(), f.getNameEn(), fallback(f.getNameEn(), f.getNameRu())))
                .nameRu(f.getNameRu())
                .nameEn(f.getNameEn())
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

    private ContentLabelDto skillLabel(ContentSkill s, String lang) {
        if (s == null) {
            return null;
        }
        return ContentLabelDto.builder()
                .id(s.getId())
                .slug(s.getSlug())
                .name(Localization.pick(lang, s.getNameRu(), s.getNameEn(), fallback(s.getNameEn(), s.getNameRu())))
                .nameRu(s.getNameRu())
                .nameEn(s.getNameEn())
                .build();
    }

    private String fallback(String preferred, String other) {
        return preferred != null ? preferred : other;
    }
}
