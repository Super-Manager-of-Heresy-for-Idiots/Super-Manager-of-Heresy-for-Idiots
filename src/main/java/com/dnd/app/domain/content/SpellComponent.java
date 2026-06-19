package com.dnd.app.domain.content;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Embeddable row of the {@code spell_component} table, owned by a {@code Spell} via
 * {@code @ElementCollection}. The {@code cost_money_value_id} column is intentionally
 * left unmapped here (read model does not surface component cost).
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpellComponent {

    @Column(name = "component_slug", nullable = false, columnDefinition = "text")
    private String componentSlug;

    @Column(name = "material_text", columnDefinition = "text")
    private String materialText;

    @Column(name = "consumed")
    private Boolean consumed;
}
