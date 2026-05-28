package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

@Entity
@Table(name = "character_class_levels")
@IdClass(CharacterClassLevelId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterClassLevel implements Persistable<CharacterClassLevelId> {

    @Id
    @Column(name = "character_id")
    private UUID characterId;

    @Id
    @Column(name = "class_id")
    private UUID classId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "character_id", insertable = false, updatable = false)
    private PlayerCharacter character;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", insertable = false, updatable = false)
    private CharacterClass characterClass;

    @Column(name = "class_level", nullable = false)
    @Builder.Default
    private Integer classLevel = 1;

    @Transient
    @Builder.Default
    private boolean newEntity = true;

    @Override
    public CharacterClassLevelId getId() {
        return new CharacterClassLevelId(characterId, classId);
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newEntity = false;
    }
}
