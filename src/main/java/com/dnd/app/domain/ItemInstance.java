package com.dnd.app.domain;

import com.dnd.app.domain.content.EquipmentItem;
import com.dnd.app.domain.content.MagicItem;
import com.dnd.app.domain.content.WeaponStat;
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

    /**
     * An instance references exactly one item source (enforced by chk_iteminst_one_ref):
     * a new-content {@link #equipmentItem} or {@link #magicItem}, or the legacy
     * {@link #template} (kept for backward-compatibility with pre-migration rows).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ItemTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_item_id")
    private EquipmentItem equipmentItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magic_item_id")
    private MagicItem magicItem;

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
        return customName != null ? customName : getBaseName();
    }

    /** Name from whichever item source is set (Russian-preferred for content items). */
    public String getBaseName() {
        if (template != null) {
            return template.getName();
        }
        if (equipmentItem != null) {
            return equipmentItem.getNameRu() != null ? equipmentItem.getNameRu() : equipmentItem.getNameEn();
        }
        if (magicItem != null) {
            return magicItem.getNameRu() != null ? magicItem.getNameRu() : magicItem.getNameEn();
        }
        return null;
    }

    /** The id of the active item source (template / equipment / magic). */
    public UUID getReferenceId() {
        if (template != null) {
            return template.getId();
        }
        if (equipmentItem != null) {
            return equipmentItem.getId();
        }
        if (magicItem != null) {
            return magicItem.getId();
        }
        return null;
    }

    /** WeaponStat of the equipped item, when it is an equipment-model weapon. */
    public WeaponStat getWeaponStat() {
        return equipmentItem != null ? equipmentItem.getWeaponStat() : null;
    }
}
