package com.dnd.app.dto.content;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс LevelUpRequest описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "LevelUpRequestV2", description = "Level-up command (new content model)")
public class LevelUpRequest {

    @NotNull(message = "ID класса обязателен")
    @Schema(description = "Class being leveled")
    private UUID classId;

    @Valid
    @Schema(description = "Reward-group selections being committed")
    private List<GroupSelection> selections;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "LevelUpGroupSelection", description = "Selection within one reward group")
    public static class GroupSelection {

        @NotNull(message = "ID группы наград обязателен")
        @Schema(description = "Reward group id being resolved")
        private UUID rewardGroupId;

        @Schema(description = "Chosen option ids (size constrained by chooseMin/chooseMax)")
        private List<UUID> optionIds;

        @Valid
        @Schema(description = "Child selections satisfying grant filters")
        private ChildSelections childSelections;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "LevelUpChildSelections")
    public static class ChildSelections {

        @Schema(description = "Ability score increases chosen for ABILITY_SCORE grants")
        private List<AbilityScoreChoice> abilityScores;

        @Schema(description = "Skill ids chosen for SKILL_PROFICIENCY grants")
        private List<UUID> skillIds;

        @Schema(description = "Spell ids chosen for SPELL grants")
        private List<UUID> spellIds;

        @Schema(description = "Feat id chosen for a FIXED/ANY FEAT grant")
        private UUID featId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(name = "LevelUpAbilityScoreChoice")
    public static class AbilityScoreChoice {
        @NotNull(message = "ID характеристики обязателен")
        private UUID abilityScoreId;
        @NotNull(message = "Размер повышения обязателен")
        private Integer amount;
    }
}
