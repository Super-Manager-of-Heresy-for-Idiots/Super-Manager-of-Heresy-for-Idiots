package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс ProficiencySkill описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "proficiency_skills")
// Phase 12: archive-only — read for audit/runtime-data migration; new flows use ContentSkill.
@Deprecated(forRemoval = false)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
/**
 * Класс ProficiencySkill описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public class ProficiencySkill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Column(name = "name_engloc", columnDefinition = "text")
    private String nameEngloc;

    @Column(name = "name_rusloc", columnDefinition = "text")
    private String nameRusloc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "governing_stat_id", nullable = false, foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private StatType governingStat;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deprecated = false;
}
