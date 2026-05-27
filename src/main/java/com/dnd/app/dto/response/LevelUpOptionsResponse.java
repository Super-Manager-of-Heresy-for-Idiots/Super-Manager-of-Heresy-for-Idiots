package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelUpOptionsResponse {
    private Integer currentTotalLevel;
    private Long xpToNextLevel;
    private List<AvailableClassOption> availableClasses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AvailableClassOption {
        private java.util.UUID classId;
        private String className;
        private Integer currentLevelInClass;
        private Integer newLevelInClass;
        private List<RewardGroup> rewardGroups;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardGroup {
        private String rewardType;
        private Boolean isChoice;
        private List<RewardEntry> rewards;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardEntry {
        private java.util.UUID rewardEntryId;
        private java.util.UUID rewardId;
        private String name;
        private String description;
        private Boolean alreadyAcquired;
    }
}
