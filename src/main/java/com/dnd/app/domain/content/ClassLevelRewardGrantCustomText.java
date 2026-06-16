package com.dnd.app.domain.content;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "class_level_reward_grant_custom_text")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClassLevelRewardGrantCustomText {

    @Id
    @Column(name = "reward_grant_id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "reward_grant_id")
    private ClassLevelRewardGrant grant;

    @Column(name = "title_ru", columnDefinition = "text")
    private String titleRu;

    @Column(name = "title_en", columnDefinition = "text")
    private String titleEn;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "user_editable", nullable = false)
    @Builder.Default
    private Boolean userEditable = true;
}
