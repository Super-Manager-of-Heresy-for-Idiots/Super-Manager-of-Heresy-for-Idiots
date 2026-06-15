package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignBlueprintDetailResponse {
    private UUID id;
    private String title;
    private String loreDescription;
    private String universeSlug;
    private String universeName;
    private String status;
    private Integer version;
    private Boolean allowForks;
    private Integer downloadCount;
    private String authorUsername;
    private String coverUrl;
    private List<String> tags;
    private Instant createdAt;
    private Instant publishedAt;
    private Boolean isDeleted;
    private UUID parentId;
    private Integer originVersion;

    private List<NpcSummary> npcs;
    private List<QuestSummary> quests;
    private List<LocationSummary> locations;
    private List<HomebrewSummary> homebrew;
    private List<PreBuiltCharacterSummary> preBuiltCharacters;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NpcSummary {
        private UUID id;
        private String name;
        private Boolean isVisibleToPlayers;
        private String sourceType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QuestSummary {
        private UUID id;
        private String title;
        private String status;
        private Boolean isVisibleToPlayers;
        private int rewardCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationSummary {
        private UUID id;
        private String name;
        private Boolean isVisibleToPlayers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HomebrewSummary {
        private UUID packageId;
        private String title;
        private Integer pinnedVersion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PreBuiltCharacterSummary {
        private UUID id;
        private String name;
        private Integer totalLevel;
    }
}
