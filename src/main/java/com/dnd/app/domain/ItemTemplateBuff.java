package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "item_template_buffs",
        uniqueConstraints = @UniqueConstraint(columnNames = {"template_id", "buff_debuff_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemTemplateBuff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private ItemTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buff_debuff_id", nullable = false)
    private BuffDebuff buffDebuff;
}
