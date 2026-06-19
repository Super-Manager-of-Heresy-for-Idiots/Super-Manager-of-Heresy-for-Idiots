package com.dnd.app.domain.content;

import com.dnd.app.domain.HomebrewPackage;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "magic_item_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MagicItemType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "magic_item_type_id")
    private UUID id;

    @Column(nullable = false, columnDefinition = "text")
    private String slug;

    @Column(name = "name_ru", nullable = false, columnDefinition = "text")
    private String nameRu;

    @Column(name = "name_en", columnDefinition = "text")
    private String nameEn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "homebrew_id")
    private HomebrewPackage homebrew;
}
