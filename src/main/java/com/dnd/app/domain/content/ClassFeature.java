package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "class_feature")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "class_feature_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ContentCharacterClass characterClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subclass_id")
    private ContentSubclass subclass;

    @Column(nullable = false, columnDefinition = "text")
    private String slug;

    private Integer level;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "activation_type", length = 32)
    private String activationType;

    @Column(name = "is_attack_roll", nullable = false)
    @Builder.Default
    private Boolean attackRoll = false;

    @Column(name = "save_ability", length = 32)
    private String saveAbility;

    @Column(name = "damage_dice", length = 40)
    private String damageDice;

    @Column(name = "damage_type", length = 32)
    private String damageType;

    @Column(name = "healing_dice", length = 40)
    private String healingDice;

    @Column(name = "healing_flat")
    private Integer healingFlat;

    @Column(name = "is_warning", nullable = false)
    @Builder.Default
    private Boolean warning = false;

    @Column(name = "warning_reason", columnDefinition = "text")
    private String warningReason;
}
