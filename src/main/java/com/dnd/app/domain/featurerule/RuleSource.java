package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * A game-facing source (sourcebook / supplement / source pack) belonging to a {@link Ruleset}.
 * Distinct from {@code feature_rule.source} (technical provenance: manual/parser/seed/migration).
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
