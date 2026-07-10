package com.dnd.app.domain.content;

import com.dnd.app.domain.Spell;
import com.dnd.app.domain.SpellSchool;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс ClassLevelRewardGrantSpell описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "class_level_reward_grant_spell")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassLevelRewardGrantSpell {

    @Id
    @Column(name = "reward_grant_id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "reward_grant_id")
    private ClassLevelRewardGrant grant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spell_id")
    private Spell spell;

    @Column(name = "spell_level")
    private Integer spellLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private SpellSchool school;

    @Column(name = "choose_count", nullable = false)
    @Builder.Default
    private Integer chooseCount = 1;

    @Column(name = "raw_filter_text", columnDefinition = "text")
    private String rawFilterText;
}
