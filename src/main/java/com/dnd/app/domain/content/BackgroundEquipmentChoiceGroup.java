package com.dnd.app.domain.content;

import com.dnd.app.domain.Background;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "background_equipment_choice_group")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackgroundEquipmentChoiceGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "background_equipment_choice_group_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "background_id", nullable = false)
    private Background background;

    @Column(name = "group_slug", nullable = false, columnDefinition = "text")
    private String groupSlug;

    @Column(name = "choose_count")
    private Integer chooseCount;

    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;

    @OneToMany(mappedBy = "group", fetch = FetchType.LAZY)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<BackgroundEquipmentOption> options = new ArrayList<>();
}
