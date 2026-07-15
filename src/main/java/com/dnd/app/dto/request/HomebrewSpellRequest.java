package com.dnd.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * DTO HomebrewSpellRequest — тело авторинга homebrew-заклинания (P2-1, Phase A: идентичность). Механика (урон/спасбросок/
 * эффекты) исполняется движком feature-rules (owner_type=SPELL) и добавляется отдельно; здесь — паспорт заклинания
 * (название, уровень, школа, время каста, дистанция, длительность, компоненты-описание, доступные классы).
 */
@Data
public class HomebrewSpellRequest {

    @NotBlank(message = "Название заклинания обязательно")
    @Size(max = 120)
    private String name;

    private String nameEn;

    @Min(0) @Max(9)
    private int level;

    /** Slug школы магии (резолвится). */
    @NotBlank(message = "Школа магии обязательна")
    private String school;

    /** Время сотворения (текст, напр. «1 действие», «1 бонусное действие»). */
    private String castingTimeRaw;

    private Boolean ritual;

    /** Дистанция (текст, напр. «60 футов», «На себя»). */
    private String rangeText;

    /** Длительность (текст, напр. «Мгновенная», «Концентрация, до 1 минуты»). */
    private String durationText;

    private Boolean concentration;

    private String description;

    /** Текст «На больших уровнях» (апкаст). */
    private String higherLevels;

    /** Классы, которым доступно заклинание (spell_class). */
    private List<UUID> availableToClassIds;
}
