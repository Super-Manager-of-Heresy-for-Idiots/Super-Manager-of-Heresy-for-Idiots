package com.dnd.app.domain.content;

import com.dnd.app.domain.HomebrewPackage;
import com.dnd.app.domain.Rarity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Класс MagicItem описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "magic_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MagicItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "magic_item_id")
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
    @JoinColumn(name = "magic_item_type_id")
    private MagicItemType type;

    @Column(name = "type_restriction_raw", columnDefinition = "text")
    private String typeRestrictionRaw;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rarity_id")
    private Rarity rarity;

    @Column(name = "variable_rarity", nullable = false)
    @Builder.Default
    private Boolean variableRarity = false;

    @Column(name = "attunement_required", nullable = false)
    @Builder.Default
    private Boolean attunementRequired = false;

    @Column(name = "attunement_requirement", columnDefinition = "text")
    private String attunementRequirement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cost_money_value_id")
    private MoneyValue cost;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "embedded_tables_detected", nullable = false)
    @Builder.Default
    private Boolean embeddedTablesDetected = false;

    @Column(columnDefinition = "text")
    private String url;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;

    @OneToMany(mappedBy = "magicItem", fetch = FetchType.LAZY)
    @Builder.Default
    private List<MagicItemAllowedEquipment> allowedEquipment = new ArrayList<>();
}
