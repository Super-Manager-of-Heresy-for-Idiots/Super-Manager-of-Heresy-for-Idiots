package com.dnd.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateClassLevelRewardRequest {

    @NotNull(message = "Required level is required")
    @Min(value = 1, message = "Required level must be between 1 and 20")
    @Max(value = 20, message = "Required level must be between 1 and 20")
    private Integer requiredLevel;

    @NotBlank(message = "Reward type is required")
    private String rewardType;

    @NotNull(message = "Reward ID is required")
    private UUID rewardId;

    private Boolean isChoice;
}
