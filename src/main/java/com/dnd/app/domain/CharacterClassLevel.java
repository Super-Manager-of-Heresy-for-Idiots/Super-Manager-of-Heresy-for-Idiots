package com.dnd.app.domain;

import com.dnd.app.domain.content.ContentCharacterClass;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.Persistable;

import java.util.UUID;

/**
 * Класс CharacterClassLevel описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "character_class_levels")
@IdClass(CharacterClassLevelId.class)
@Deprecated(forRemoval = false)
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
    @JoinColumn(name = "class_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private ContentCharacterClass characterClass;

    @Column(name = "class_level", nullable = false)
    @Builder.Default
    private Integer classLevel = 1;

    @Transient
    @Builder.Default
    private boolean newEntity = true;

    /**
     * Возвращает результат операции "get id" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    @Override
    public CharacterClassLevelId getId() {
        return new CharacterClassLevelId(characterId, classId);
    }

    /**
     * Проверяет условие операции "is new" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
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
