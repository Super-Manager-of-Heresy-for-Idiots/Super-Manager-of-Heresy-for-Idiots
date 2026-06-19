package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "class_feature")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "class_feature_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ContentCharacterClass characterClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subclass_id")
    private ContentSubclass subclass;

    @Column(nullable = false, columnDefinition = "text")
    private String slug;

    private Integer level;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @Column(columnDefinition = "text")
    private String description;
}
