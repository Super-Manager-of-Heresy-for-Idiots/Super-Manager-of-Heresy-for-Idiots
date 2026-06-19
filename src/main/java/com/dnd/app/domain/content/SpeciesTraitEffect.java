package com.dnd.app.domain.content;

import com.dnd.app.domain.DamageType;
import com.dnd.app.domain.Spell;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "species_trait_effect")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeciesTraitEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "species_trait_effect_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "species_trait_id")
    private SpeciesTrait trait;

    @Column(name = "effect_type", nullable = false, columnDefinition = "text")
    private String effectType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "damage_type_id")
    private DamageType damageType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spell_id")
    private Spell spell;

    @Column(name = "range_ft")
    private Integer rangeFt;
}
