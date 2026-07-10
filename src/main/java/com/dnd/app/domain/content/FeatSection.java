package com.dnd.app.domain.content;

import com.dnd.app.domain.Feat;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс FeatSection описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "feat_section")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatSection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "feat_section_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feat_id", nullable = false)
    private Feat feat;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(columnDefinition = "text")
    private String title;

    @Column(columnDefinition = "text")
    private String body;
}
