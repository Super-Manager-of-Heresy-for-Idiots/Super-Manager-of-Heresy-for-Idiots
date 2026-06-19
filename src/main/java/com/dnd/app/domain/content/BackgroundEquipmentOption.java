package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "background_equipment_option")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackgroundEquipmentOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "background_equipment_option_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "background_equipment_choice_group_id", nullable = false)
    private BackgroundEquipmentChoiceGroup group;

    @Column(name = "option_code", nullable = false, columnDefinition = "text")
    private String optionCode;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;

    @OneToMany(mappedBy = "option", fetch = FetchType.LAZY)
    @Builder.Default
    private List<BackgroundEquipmentEntry> entries = new ArrayList<>();
}
