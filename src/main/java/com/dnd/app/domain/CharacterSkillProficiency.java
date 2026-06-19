package com.dnd.app.domain;

import com.dnd.app.domain.content.ContentSkill;
import com.dnd.app.domain.enums.SkillProficiencySource;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "character_skill_proficiencies",
        uniqueConstraints = @UniqueConstraint(columnNames = {"character_id", "skill_id"}))
/**
 * Character proficiency table. {@code skill_id} references the content {@code skill}
 * (ContentSkill). The FK stays NO_CONSTRAINT until existing legacy rows are migrated by
 * {@link com.dnd.app.service.RuntimeDataMigrationService}; a real FK is added afterwards.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterSkillProficiency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", nullable = false)
    private PlayerCharacter character;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ContentSkill skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SkillProficiencySource source;
}
