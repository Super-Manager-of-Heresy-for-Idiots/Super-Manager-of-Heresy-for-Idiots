package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс LevelUpResultResponse описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
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
