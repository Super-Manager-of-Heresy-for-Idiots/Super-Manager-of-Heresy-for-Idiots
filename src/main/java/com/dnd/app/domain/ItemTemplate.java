package com.dnd.app.domain;

import com.dnd.app.domain.enums.SkillActivation;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "item_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_type_id")
    private ItemType itemType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rarity_id")
    private Rarity rarity;

    @Column(name = "damage_dice", length = 10)
    private String damageDice;

    @Column(name = "damage_bonus")
    @Builder.Default
    private Integer damageBonus = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "damage_type_id")
    private DamageType damageType;

    @Column(name = "is_stackable", nullable = false)
    @Builder.Default
    private Boolean isStackable = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_activation", length = 10)
    private SkillActivation skillActivation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemTemplateBuff> templateBuffs = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
