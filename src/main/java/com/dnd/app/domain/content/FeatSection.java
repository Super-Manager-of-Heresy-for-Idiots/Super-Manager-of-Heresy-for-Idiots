package com.dnd.app.domain.content;

import com.dnd.app.domain.Feat;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

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
