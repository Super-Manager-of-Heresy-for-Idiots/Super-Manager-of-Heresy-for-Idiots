package com.dnd.app.dto.response;

import com.dnd.app.dto.content.BackgroundDetailResponse;
import com.dnd.app.dto.content.ContentClassDetailResponse;
import com.dnd.app.dto.content.FeatDetailResponse;
import com.dnd.app.dto.content.ItemDefinitionResponse;
import com.dnd.app.dto.content.SpeciesDetailResponse;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Богатый предпросмотр homebrew-пакета для витрины (флагманская фича «что я добавляю»).
 * Собирается поверх существующих detail-мапперов — механика заклинаний, статы предметов,
 * прогрессия классов, трейты видов, статблоки монстров приходят ПОЛНОСТЬЮ, чтобы игрок раскрыл
 * любую запись как в игре. «Лёгкие» типы (навыки, бафы, ресурсы, типы предметов, архетипы)
 * приходят в {@code header.contentByType} (ContentSummaryDto) — их L3 в макете компактный.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HomebrewPreviewResponse {

    /** Мета пакета + счётчики + рейтинг + contentByType (лёгкие типы). */
    private HomebrewDetailResponse header;

    /** Заклинания с полной механикой (feature-rules): урон/спас/состояния/апкаст. */
    private List<HomebrewSpellResponse> spells;
    /** Предметы (magic + equipment) единой сущностью: редкость, статы оружия/брони, аттюнмент, умение. */
    private List<ItemDefinitionResponse> items;
    /** Классы: кость хитов, владения, спеллкастинг, фичи по уровням, сабклассы. */
    private List<ContentClassDetailResponse> classes;
    /** Виды/расы: тип существа, размеры, скорости, трейты с эффектами, тёмное зрение. */
    private List<SpeciesDetailResponse> species;
    /** Черты: категория, требования, разделы описания. */
    private List<FeatDetailResponse> feats;
    /** Предыстории: характеристики, навыки, инструменты, языки, снаряжение, черта. */
    private List<BackgroundDetailResponse> backgrounds;
    /** Монстры бестиария: полный статблок (тянутся отдельно от content-items). */
    private List<MonsterResponse> monsters;
}
