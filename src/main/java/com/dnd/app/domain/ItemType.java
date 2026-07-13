package com.dnd.app.domain;

import com.dnd.app.domain.enums.SkillActivation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.util.UUID;

/**
 * Класс ItemType описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "item_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private EquipmentSlot slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;

    // Авторство (P1-4): кто создал/изменил homebrew-контент; null для ванильных строк.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "damage_dice", length = 10)
    private String damageDice;

    @Column(name = "damage_bonus", nullable = false)
    @Builder.Default
    private Integer damageBonus = 0;

    // Legacy seeds can leave a dangling damage_type_id (no matching damage_type row).
    // NotFoundAction.IGNORE resolves the missing reference to null instead of throwing
    // EntityNotFoundException on lazy init, which otherwise 500s the whole item-types listing.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "damage_type_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    @NotFound(action = NotFoundAction.IGNORE)
    private DamageType damageType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_activation", length = 10)
    private SkillActivation skillActivation;
}
