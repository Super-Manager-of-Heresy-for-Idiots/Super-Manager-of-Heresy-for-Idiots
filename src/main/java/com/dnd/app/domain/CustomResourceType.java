package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "custom_resource_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomResourceType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "max_value")
    private Integer maxValue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_bound_id")
    private CharacterClass classBound;
}
