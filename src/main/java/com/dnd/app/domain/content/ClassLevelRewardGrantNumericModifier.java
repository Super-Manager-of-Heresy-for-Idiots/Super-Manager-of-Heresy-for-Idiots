package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "class_level_reward_grant_numeric_modifier")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassLevelRewardGrantNumericModifier {

    @Id
    @Column(name = "reward_grant_id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "reward_grant_id")
    private ClassLevelRewardGrant grant;

    @Column(name = "modifier_key", nullable = false, columnDefinition = "text")
    private String modifierKey;

    @Column(name = "target_kind", nullable = false, columnDefinition = "text")
    private String targetKind;

    @Column(name = "target_label_ru", columnDefinition = "text")
    private String targetLabelRu;

    @Column(name = "target_label_en", columnDefinition = "text")
    private String targetLabelEn;

    @Column(precision = 12, scale = 3)
    private BigDecimal amount;

    @Column(name = "dice_formula_id")
    private UUID diceFormulaId;

    @Column(name = "unit_text", columnDefinition = "text")
    private String unitText;

    @Column(name = "duration_text", columnDefinition = "text")
    private String durationText;

    @Column(name = "stacking_rule", columnDefinition = "text")
    private String stackingRule;

    @Column(columnDefinition = "text")
    private String notes;
}
