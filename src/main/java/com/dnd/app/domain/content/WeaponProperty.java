package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "weapon_property")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeaponProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "weapon_property_id")
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String slug;

    @Column(name = "name_ru", nullable = false, columnDefinition = "text")
    private String nameRu;

    @Column(name = "name_en", columnDefinition = "text")
    private String nameEn;
}
