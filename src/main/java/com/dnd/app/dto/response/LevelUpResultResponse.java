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
public class LevelUpResultResponse {
    private Integer newTotalLevel;
    private String classLeveled;
    private Integer newClassLevel;
    private Integer hpIncrease;
    private Integer newMaxHp;
    private List<AcquiredRewardSummary> rewardsAcquired;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AcquiredRewardSummary {
        private String rewardType;
        private String name;
    }
}
