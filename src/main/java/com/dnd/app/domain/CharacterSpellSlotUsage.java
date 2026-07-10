package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс CharacterSpellSlotUsage описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
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
