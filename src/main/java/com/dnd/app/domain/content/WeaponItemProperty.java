package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "weapon_item_property")
@IdClass(WeaponItemPropertyId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeaponItemProperty {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_item_id")
    private EquipmentItem equipmentItem;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weapon_property_id")
    private WeaponProperty weaponProperty;

    @Column(name = "normal_range_ft")
    private Integer normalRangeFt;

    @Column(name = "long_range_ft")
    private Integer longRangeFt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "versatile_dice_formula_id")
    private DiceFormula versatileDiceFormula;

    @Column(name = "ammunition_equipment_item_id")
    private UUID ammunitionEquipmentItemId;

    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;
}
