package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "feat_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "feat_category_id")
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String slug;

    @Column(name = "name_ru", nullable = false, columnDefinition = "text")
    private String nameRu;

    @Column(name = "name_en", columnDefinition = "text")
    private String nameEn;
}
