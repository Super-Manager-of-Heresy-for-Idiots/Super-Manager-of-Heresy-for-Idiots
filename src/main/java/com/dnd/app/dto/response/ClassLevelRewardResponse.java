package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassLevelRewardResponse {
    private UUID id;
    private UUID classId;
    private Integer requiredLevel;
    private String rewardType;
    private UUID rewardId;
    private String rewardName;
    private Boolean isChoice;
}
