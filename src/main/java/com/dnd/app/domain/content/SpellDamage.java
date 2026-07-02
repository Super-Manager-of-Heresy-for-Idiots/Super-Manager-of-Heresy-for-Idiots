package com.dnd.app.domain.content;

import com.dnd.app.domain.DamageType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;

/**
 * Embeddable row of the {@code spell_damage} table, owned by a {@code Spell} via
 * {@code @ElementCollection}. Structured base damage lifted from the normalized
 * source's {@code detected_damage[]}: the dice formula (canonicalised to {@code NdM}),
 * the damage type reference, and the original raw text for provenance.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpellDamage {

    @Column(name = "dice", columnDefinition = "text")
    private String dice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "damage_type_id")
    private DamageType damageType;

    @Column(name = "raw", columnDefinition = "text")
    private String raw;
}
