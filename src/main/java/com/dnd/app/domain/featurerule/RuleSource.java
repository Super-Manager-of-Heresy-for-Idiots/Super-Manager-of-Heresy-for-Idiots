package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс RuleSource описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "rule_source")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleSource {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 48, unique = true)
    private String key;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    /** e.g. core, supplement, source_pack, homebrew, third_party. */
    @Column(name = "source_type", length = 24)
    private String sourceType;

    @Column(name = "ruleset_id")
    private UUID rulesetId;
}
