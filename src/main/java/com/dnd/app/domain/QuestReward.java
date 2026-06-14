package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "quest_rewards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestReward {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quest_id", nullable = false)
    private CampaignQuest quest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_template_id")
    private ItemTemplate itemTemplate;

    @Column
    @Builder.Default
    private Integer quantity = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_type_id")
    private CurrencyType currencyType;

    @Column(name = "currency_amount", precision = 15, scale = 2)
    private java.math.BigDecimal currencyAmount;

    @Column(name = "xp_amount")
    private Integer xpAmount;
}
