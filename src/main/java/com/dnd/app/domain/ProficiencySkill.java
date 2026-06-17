package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "proficiency_skills")
/**
 * Legacy proficiency skill catalog mapped to the old plural content table.
 * New proficiency content must use {@link com.dnd.app.domain.content.ContentSkill}.
 */
// Phase 12: archive-only — read for audit/runtime-data migration; new flows use ContentSkill.
@Deprecated(forRemoval = false)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
