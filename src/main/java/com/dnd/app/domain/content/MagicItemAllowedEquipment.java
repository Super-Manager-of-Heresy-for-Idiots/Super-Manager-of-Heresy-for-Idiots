package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "magic_item_allowed_equipment")
@IdClass(MagicItemAllowedEquipmentId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MagicItemAllowedEquipment {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magic_item_id")
    private MagicItem magicItem;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_item_id")
    private EquipmentItem equipmentItem;

    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;
}
