package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "blueprint_quest_rewards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlueprintReward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private BlueprintQuest quest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_template_id")
    private ItemTemplate itemTemplate;

    @Column
    @Builder.Default
    private Integer quantity = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_type_id", foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private CurrencyType currencyType;

    @Column(name = "currency_amount", precision = 15, scale = 2)
    private java.math.BigDecimal currencyAmount;

    @Column(name = "xp_amount")
    private Integer xpAmount;
}
