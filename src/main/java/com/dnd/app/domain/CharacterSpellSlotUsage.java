package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Tracks how many spell slots a character has SPENT at a given spell level. The maximum
 * number of slots is always derived from class progression and never stored here; this
 * table holds only consumption so that available = derivedMax - expendedCount.
 */
@Entity
@Table(name = "character_spell_slot_usage")
@IdClass(CharacterSpellSlotUsageId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CharacterSpellSlotUsage {

    @Id
    @Column(name = "character_id")
    private UUID characterId;

    @Id
    @Column(name = "spell_level")
    private Integer spellLevel;

    @Column(name = "expended_count", nullable = false)
    @Builder.Default
    private int expendedCount = 0;
}
