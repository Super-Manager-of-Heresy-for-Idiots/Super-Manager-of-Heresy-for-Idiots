package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Структурные условия активности item-правила: экипировка, аттюнмент и потребление предмета.
 */
@Entity
@Table(name = "feature_item_binding")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureItemBinding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_rule_id", nullable = false, unique = true)
    private UUID featureRuleId;

    @Column(name = "requires_equipped", nullable = false)
    @Builder.Default
    private boolean requiresEquipped = false;

    @Column(name = "requires_attunement", nullable = false)
    @Builder.Default
    private boolean requiresAttunement = false;

    @Column(name = "consume_on_use", nullable = false)
    @Builder.Default
    private boolean consumeOnUse = false;

    @Column(name = "consume_quantity", nullable = false)
    @Builder.Default
    private Integer consumeQuantity = 1;

    @Column(name = "allowed_slot_code", length = 60)
    private String allowedSlotCode;
}
