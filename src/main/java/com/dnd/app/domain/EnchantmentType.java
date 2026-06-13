package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "enchantment_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EnchantmentType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "damage_dice", length = 10)
    private String damageDice;

    @Column(name = "damage_bonus", nullable = false)
    @Builder.Default
    private Integer damageBonus = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "damage_type_id")
    private DamageType damageType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buff_debuff_id")
    private BuffDebuff buffDebuff;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
