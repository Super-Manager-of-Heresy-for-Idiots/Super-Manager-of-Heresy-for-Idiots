package com.dnd.app.domain.content;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Класс SpellComponent описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
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
