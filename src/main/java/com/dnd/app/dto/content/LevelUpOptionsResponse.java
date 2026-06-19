package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Final level-up options model (new content model). Surfaces the reward groups,
 * options and grants available for the next level of each eligible class, plus
 * already-selected state and derived data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LevelUpOptions", description = "Available level-up options (new content model)")
public class LevelUpOptionsResponse {

    @Schema(description = "Character total level before leveling", example = "3")
    private Integer currentTotalLevel;

    @Schema(description = "XP remaining to next character level")
    private Long xpToNextLevel;

    @Schema(description = "Eligible classes to level into")
    private List<AvailableClassOption> availableClasses;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "LevelUpClassOption", description = "Level-up option for one class")
    public static class AvailableClassOption {

        @Schema(description = "Class id")
        private UUID classId;

        @Schema(description = "Class display name", example = "Stormbinder")
        private String className;

        @Schema(description = "Current level in this class", example = "2")
        private Integer currentLevelInClass;

        @Schema(description = "Level in this class after leveling", example = "3")
        private Integer newLevelInClass;

        @Schema(description = "HP gain info for this choice")
        private HpGain hpGain;

        @Schema(description = "Derived stat changes")
        private Derived derived;

        @Schema(description = "Reward groups unlocked at the new class level")
        private List<RewardGroupDto> rewardGroups;

        @Schema(description = "Selections already made/acquired for these groups")
        private List<SelectedState> alreadySelected;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "LevelUpHpGain")
    public static class HpGain {
        private Integer hitDie;
        private Integer averageGain;
        private Integer conModifier;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "LevelUpDerived")
    public static class Derived {
        private Integer newTotalLevel;
        private Integer newClassLevel;
        private Integer proficiencyBonusBefore;
        private Integer proficiencyBonusAfter;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "LevelUpSelectedState", description = "An already-recorded reward selection")
    public static class SelectedState {
        private UUID rewardGroupId;
        private UUID rewardOptionId;
    }
}
