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

    @Column(name = "name_engloc", columnDefinition = "text")
    private String nameEngloc;

    @Column(name = "name_rusloc", columnDefinition = "text")
    private String nameRusloc;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "description_engloc", columnDefinition = "text")
    private String descriptionEngloc;

    @Column(name = "description_rusloc", columnDefinition = "text")
    private String descriptionRusloc;

    @Column(name = "skill_proficiency_ids_json", columnDefinition = "text")
    private String skillProficiencyIdsJson;

    @Column(name = "granted_extras", columnDefinition = "text")
    private String grantedExtras;

    @Column(name = "granted_extras_engloc", columnDefinition = "text")
    private String grantedExtrasEngloc;

    @Column(name = "granted_extras_rusloc", columnDefinition = "text")
    private String grantedExtrasRusloc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;
}
