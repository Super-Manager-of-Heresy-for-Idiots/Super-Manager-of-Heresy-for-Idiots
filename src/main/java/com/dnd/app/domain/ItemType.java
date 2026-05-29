package com.dnd.app.domain;

import com.dnd.app.domain.enums.DamageType;
import com.dnd.app.domain.enums.EquipmentSlot;
import com.dnd.app.domain.enums.SkillActivation;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EquipmentSlot slot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_homebrew_id")
    private HomebrewPackage sourceHomebrew;

    @Column(name = "damage_dice", length = 10)
    private String damageDice;

    @Column(name = "damage_bonus", nullable = false)
    @Builder.Default
    private Integer damageBonus = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "damage_type", length = 20)
    private DamageType damageType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_activation", length = 10)
    private SkillActivation skillActivation;
}
