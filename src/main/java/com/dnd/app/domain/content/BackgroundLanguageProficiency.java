package com.dnd.app.domain.content;

import com.dnd.app.domain.Background;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "background_language_proficiency")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackgroundLanguageProficiency {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "background_language_proficiency_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "background_id")
    private Background background;

    @Column(name = "language_slug", columnDefinition = "text")
    private String languageSlug;

    @Column(name = "choose_count")
    private Integer chooseCount;

    @Column(name = "raw_text", columnDefinition = "text")
    private String rawText;
}
