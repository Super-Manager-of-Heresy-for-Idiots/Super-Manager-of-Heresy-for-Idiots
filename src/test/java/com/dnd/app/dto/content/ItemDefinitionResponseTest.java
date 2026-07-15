package com.dnd.app.dto.content;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Юнит-тесты чистых фабрик {@link ItemDefinitionResponse} (IT-1): проверяют, что единый DTO
 * корректно собирается из per-kind detail-DTO (magic/equipment) — дискриминатор kind, общие поля,
 * стоимость и kind-специфичные секции.
 */
class ItemDefinitionResponseTest {

    @Test
    void fromMagic_mapsCommonFieldsAndAttunement() {
        UUID id = UUID.randomUUID();
        UUID pkg = UUID.randomUUID();
        MagicItemDetailResponse magic = MagicItemDetailResponse.builder()
                .id(id)
                .slug("flame-tongue")
                .name("Flame Tongue")
                .nameRu("Огненный язык")
                .description("A burning blade.")
                .rarity(ContentLabelDto.builder().slug("rare").name("Rare").build())
                .attunementRequired(true)
                .attunementRequirement("by a warrior")
                .cost(MagicItemDetailResponse.CostDto.builder()
                        .amount(new BigDecimal("500"))
                        .copperValue(new BigDecimal("50000"))
                        .build())
                .packageId(pkg)
                .source("HOMEBREW")
                .homebrewTitle("My Pack")
                .build();

        ItemDefinitionResponse def = ItemDefinitionResponse.fromMagic(magic);

        assertThat(def.getKind()).isEqualTo("MAGIC");
        assertThat(def.getId()).isEqualTo(id);
        assertThat(def.getName()).isEqualTo("Flame Tongue");
        assertThat(def.getRarity().getSlug()).isEqualTo("rare");
        assertThat(def.getAttunementRequired()).isTrue();
        assertThat(def.getAttunementRequirement()).isEqualTo("by a warrior");
        assertThat(def.getCost().getCopperValue()).isEqualByComparingTo("50000");
        assertThat(def.getSource()).isEqualTo("HOMEBREW");
        assertThat(def.getPackageId()).isEqualTo(pkg);
        // магический предмет не несёт секций снаряжения
        assertThat(def.getWeaponStat()).isNull();
        assertThat(def.getArmorStat()).isNull();
        assertThat(def.getEquipmentKind()).isNull();
    }

    @Test
    void fromEquipment_mapsKindSectionsAndWeight() {
        UUID id = UUID.randomUUID();
        EquipmentItemDetailResponse.WeaponStatDto weaponStat = EquipmentItemDetailResponse.WeaponStatDto.builder()
                .flatDamage(3)
                .build();
        EquipmentItemDetailResponse equip = EquipmentItemDetailResponse.builder()
                .id(id)
                .slug("longsword")
                .name("Longsword")
                .kind("weapon")
                .category(ContentLabelDto.builder().slug("martial").name("Martial").build())
                .weightLb(new BigDecimal("3"))
                .weaponStat(weaponStat)
                .weaponProperties(List.of())
                .source("GLOBAL")
                .build();

        ItemDefinitionResponse def = ItemDefinitionResponse.fromEquipment(equip);

        assertThat(def.getKind()).isEqualTo("EQUIPMENT");
        assertThat(def.getId()).isEqualTo(id);
        assertThat(def.getEquipmentKind()).isEqualTo("weapon");
        assertThat(def.getType().getSlug()).isEqualTo("martial"); // category → type
        assertThat(def.getWeightLb()).isEqualByComparingTo("3");
        assertThat(def.getWeaponStat()).isSameAs(weaponStat);
        assertThat(def.getSource()).isEqualTo("GLOBAL");
        // снаряжение не несёт аттюнмента
        assertThat(def.getAttunementRequired()).isNull();
    }
}
