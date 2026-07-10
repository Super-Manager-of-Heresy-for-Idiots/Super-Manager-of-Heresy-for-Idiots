package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Класс BackgroundEquipmentEntry описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "background_equipment_entry")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackgroundEquipmentEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "background_equipment_entry_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "background_equipment_option_id", nullable = false)
    private BackgroundEquipmentOption option;

    @Column(name = "entry_type", nullable = false, columnDefinition = "text")
    private String entryType;

    @Column(name = "equipment_item_id")
    private UUID equipmentItemId;

    @Column(name = "money_value_id")
    private UUID moneyValueId;

    @Column(nullable = false)
    @Builder.Default
    private BigDecimal quantity = BigDecimal.ONE;

    @Column(name = "quantity_unit_raw", columnDefinition = "text")
    private String quantityUnitRaw;

    @Column(name = "variant_note", columnDefinition = "text")
    private String variantNote;

    @Column(name = "choice_ref", columnDefinition = "text")
    private String choiceRef;

    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;
}
