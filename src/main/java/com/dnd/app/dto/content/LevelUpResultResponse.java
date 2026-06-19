package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Final level-up result (new content model). Reports derived changes, the grants
 * that were applied deterministically, and any manual action items the player must
 * complete on their sheet (non-deterministic/custom grants are not auto-applied).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LevelUpResult", description = "Result of committing a level-up (new content model)")
public class LevelUpResultResponse {

    @Schema(description = "Total character level after leveling", example = "3")
    private Integer newTotalLevel;

    @Schema(description = "Class that was leveled", example = "Stormbinder")
    private String classLeveled;

    @Schema(description = "New level in that class", example = "3")
    private Integer newClassLevel;

    private Integer hpIncrease;
    private Integer newMaxHp;
    private Integer proficiencyBonusBefore;
    private Integer proficiencyBonusAfter;

    @Schema(description = "Grants applied deterministically")
    private List<AppliedGrant> appliedGrants;

    @Schema(description = "Manual items the player must complete (custom/non-deterministic grants)")
    private List<ManualActionItem> manualActions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "LevelUpAppliedGrant")
    public static class AppliedGrant {
        private UUID grantId;
        private String grantType;
        private String summary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "LevelUpManualActionItem")
    public static class ManualActionItem {
        private UUID grantId;
        private String grantType;
        private String instruction;
    }
}
