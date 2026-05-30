package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "item_enchantments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"item_instance_id", "enchantment_type_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemEnchantment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_instance_id", nullable = false)
    private ItemInstance itemInstance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enchantment_type_id", nullable = false)
    private EnchantmentType enchantmentType;

    @CreationTimestamp
    @Column(name = "applied_at", nullable = false, updatable = false)
    private Instant appliedAt;

    @Column(length = 255)
    private String notes;
}
