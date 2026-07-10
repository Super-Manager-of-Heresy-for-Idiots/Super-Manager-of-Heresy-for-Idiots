package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс FeatureLanguageGrant описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "feature_language_grant")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureLanguageGrant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "feature_rule_id", nullable = false)
    private UUID featureRuleId;

    /** Specific language id; null when it is a choice governed by a filter. */
    @Column(name = "language_id")
    private UUID languageId;

    @Column(name = "filter_rule_id")
    private UUID filterRuleId;

    /** {@link GrantTiming} code. */
    @Column(name = "grant_timing", nullable = false, length = 24)
    @Builder.Default
    private String grantTiming = GrantTiming.LEVEL_UP.getCode();
}
