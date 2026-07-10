package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Класс BuffDebuff описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "buffs_debuffs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BuffDebuff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "effect_type", nullable = false, length = 30)
    private String effectType;

    // Legacy seeds can leave a dangling target_stat_id (no matching stat_type row).
    // NotFoundAction.IGNORE resolves the missing reference to null instead of throwing
    // EntityNotFoundException on lazy init, which otherwise 500s the whole buffs listing
    // (and the spell buff-link picker, which reads the same list).
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_stat_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @NotFound(action = NotFoundAction.IGNORE)
    private StatType targetStat;

    @Column(name = "modifier_value")
    private Integer modifierValue;

    @Column(name = "duration_rounds")
    private Integer durationRounds;

    @Column(name = "is_buff", nullable = false)
    private Boolean isBuff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean deprecated = false;
}
