package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_enchantments", uniqueConstraints = {
        @UniqueConstraint(name = "uq_slot_enchantment", columnNames = {"inventory_slot_id", "enchantment_type_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryEnchantment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_slot_id", nullable = false)
    private InventorySlot inventorySlot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enchantment_type_id", nullable = false)
    private EnchantmentType enchantmentType;

    @Column(name = "applied_at", nullable = false)
    @Builder.Default
    private Instant appliedAt = Instant.now();

    @Column(length = 255)
    private String notes;
}
