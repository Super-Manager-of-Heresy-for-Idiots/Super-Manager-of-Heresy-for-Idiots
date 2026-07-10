package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс SpeciesSpeed описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "species_speed")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SpeciesSpeed {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "species_speed_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "species_id")
    private Species species;

    @Column(name = "speed_type_slug", nullable = false, columnDefinition = "text")
    private String speedTypeSlug;

    @Column(name = "amount_ft")
    private Integer amountFt;

    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;
}
