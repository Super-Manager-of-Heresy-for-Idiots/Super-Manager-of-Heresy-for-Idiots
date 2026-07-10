package com.dnd.app.domain.content;

import com.dnd.app.domain.Background;
import com.dnd.app.domain.Feat;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс BackgroundFeatOption описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "background_feat_option")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackgroundFeatOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "background_feat_option_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "background_id", nullable = false)
    private Background background;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feat_id")
    private Feat feat;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feat_category_id")
    private FeatCategory featCategory;

    @Column(name = "choose_count", nullable = false)
    @Builder.Default
    private Integer chooseCount = 1;

    @Column(name = "selected_option_raw", columnDefinition = "text")
    private String selectedOptionRaw;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommended_feat_id")
    private Feat recommendedFeat;

    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;
}
