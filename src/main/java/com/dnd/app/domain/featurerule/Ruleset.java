package com.dnd.app.domain.featurerule;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Класс Ruleset описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Entity
@Table(name = "ruleset")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ruleset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 48, unique = true)
    private String key;

    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(length = 16)
    private String edition;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;
}
