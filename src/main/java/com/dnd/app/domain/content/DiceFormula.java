package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс DiceFormula описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "dice_formula")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiceFormula {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "dice_formula_id")
    private UUID id;

    @Column(name = "dice_count")
    private Integer diceCount;

    @Column(name = "die_size")
    private Integer dieSize;

    @Column(name = "bonus")
    private Integer bonus;

    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;
}
