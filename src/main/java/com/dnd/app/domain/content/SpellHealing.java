package com.dnd.app.domain.content;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

/**
 * Класс SpellHealing описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
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
