package com.dnd.app.domain.content;

import com.dnd.app.domain.HomebrewPackage;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "equipment_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "equipment_item_id")
    private UUID id;

    @Column(name = "mod_id")
    private UUID modId;

    @Column(name = "source_id")
    private UUID sourceId;

    @Column(nullable = false, columnDefinition = "text")
    private String slug;

    @Column(name = "name_ru", nullable = false, columnDefinition = "text")
    private String nameRu;

    @Column(name = "name_en", columnDefinition = "text")
    private String nameEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private EquipmentCategory category;

    @Column(name = "kind", nullable = false, columnDefinition = "text")
    private String kind;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_money_value_id")
    private MoneyValue cost;

    @Column(name = "weight_lb", precision = 12, scale = 4)
    private BigDecimal weightLb;

    @Column(name = "properties_text", columnDefinition = "text")
    private String propertiesText;

    @Column(columnDefinition = "text")
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;

    @OneToOne(mappedBy = "equipmentItem", fetch = FetchType.LAZY)
    private WeaponStat weaponStat;

    @OneToOne(mappedBy = "equipmentItem", fetch = FetchType.LAZY)
    private ArmorStat armorStat;

    @OneToMany(mappedBy = "equipmentItem", fetch = FetchType.LAZY)
    @Builder.Default
    private List<WeaponItemProperty> weaponProperties = new ArrayList<>();
}
