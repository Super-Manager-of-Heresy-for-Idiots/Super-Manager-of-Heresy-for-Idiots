package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс FeatureSpellFilter описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "feature_spell_filter")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureSpellFilter {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "class_id")
    private UUID classId;

    @Column(name = "school_id")
    private UUID schoolId;

    @Column(name = "max_spell_level_formula_id")
    private UUID maxSpellLevelFormulaId;

    @Column(length = 32)
    private String tag;

    @Column(name = "source_filter", length = 64)
    private String sourceFilter;
}
