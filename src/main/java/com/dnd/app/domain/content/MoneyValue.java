package com.dnd.app.domain.content;

import com.dnd.app.domain.CurrencyType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Класс MoneyValue описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "money_value")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MoneyValue {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "money_value_id")
    private UUID id;

    @Column(name = "amount", precision = 12, scale = 3)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_id")
    private CurrencyType currency;

    @Column(name = "copper_value", precision = 14, scale = 3)
    private BigDecimal copperValue;

    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;
}
