package com.dnd.app.domain.content;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Embeddable row of the {@code spell_healing} table, owned by a {@code Spell} via
 * {@code @ElementCollection}. Structured healing lifted from the RU description:
 * a dice formula (canonicalised to {@code NdM}), a flat amount, or neither (a full
 * heal such as "восстанавливает все свои Хиты"), plus the original raw text for
 * provenance. Mirrors {@link SpellDamage} but carries no damage type. The
 * "+ spellcasting modifier" tail is derived per caster and is not stored.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpellHealing {

    @Column(name = "dice", columnDefinition = "text")
    private String dice;

    @Column(name = "flat")
    private Integer flat;

    @Column(name = "raw", columnDefinition = "text")
    private String raw;
}
