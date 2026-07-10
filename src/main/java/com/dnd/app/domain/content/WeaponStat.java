package com.dnd.app.domain.content;

import com.dnd.app.domain.DamageType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс WeaponStat описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "weapon_stat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeaponStat {

    @Id
    @Column(name = "equipment_item_id")
    private UUID equipmentItemId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_item_id")
    private EquipmentItem equipmentItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "damage_dice_formula_id")
    private DiceFormula damageDiceFormula;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "damage_type_id")
    private DamageType damageType;

    @Column(name = "flat_damage")
    private Integer flatDamage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mastery_id")
    private WeaponMastery mastery;
}
