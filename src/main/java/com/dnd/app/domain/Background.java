package com.dnd.app.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "backgrounds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Background {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "skill_proficiency_ids_json", columnDefinition = "text")
    private String skillProficiencyIdsJson;

    @Column(name = "granted_extras", columnDefinition = "text")
    private String grantedExtras;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;
}
