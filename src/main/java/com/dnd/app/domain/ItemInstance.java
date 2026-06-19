package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "item_instances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ItemTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_character_id")
    private PlayerCharacter ownerCharacter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_storage_id")
    private SharedStorage sharedStorage;

    @Column(name = "custom_name", length = 100)
    private String customName;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "is_unique", nullable = false)
    @Builder.Default
    private Boolean isUnique = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id")
    private EquipmentSlot slot;

    @Column(columnDefinition = "text")
    private String notes;

    @OneToMany(mappedBy = "itemInstance", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemEnchantment> enchantments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getDisplayName() {
        return customName != null ? customName : template.getName();
    }
}
