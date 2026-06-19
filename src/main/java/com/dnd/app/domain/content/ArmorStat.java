package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "armor_stat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArmorStat {

    @Id
    @Column(name = "equipment_item_id")
    private UUID equipmentItemId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_item_id")
    private EquipmentItem equipmentItem;

    @Column(name = "base_ac")
    private Integer baseAc;

    @Column(name = "dex_bonus_allowed", nullable = false)
    @Builder.Default
    private Boolean dexBonusAllowed = false;

    @Column(name = "max_dex_bonus")
    private Integer maxDexBonus;

    @Column(name = "strength_required")
    private Integer strengthRequired;

    @Column(name = "stealth_disadvantage", nullable = false)
    @Builder.Default
    private Boolean stealthDisadvantage = false;

    @Column(name = "armor_class_raw", columnDefinition = "text")
    private String armorClassRaw;
}
