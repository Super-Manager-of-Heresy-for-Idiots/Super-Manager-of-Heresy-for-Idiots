package com.dnd.app.domain;

import com.dnd.app.domain.enums.EffectRole;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс SkillEffect описывает доменную модель, которая хранит состояние и инварианты игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "skill_effects", uniqueConstraints = {
        @UniqueConstraint(name = "uq_skill_buff", columnNames = {"skill_id", "buff_debuff_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillEffect {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buff_debuff_id", nullable = false)
    private BuffDebuff buffDebuff;

    @Enumerated(EnumType.STRING)
    @Column(name = "effect_role", nullable = false, length = 10)
    private EffectRole effectRole;

    @Column(name = "chance_percent", nullable = false)
    @Builder.Default
    private Integer chancePercent = 100;
}
