package com.dnd.app.domain;

import com.dnd.app.domain.enums.ContentType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "homebrew_content_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HomebrewContentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "package_id", nullable = false)
    private HomebrewPackage homebrewPackage;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 30)
    private ContentType contentType;

    @Column(name = "content_id", nullable = false)
    private UUID contentId;
}
