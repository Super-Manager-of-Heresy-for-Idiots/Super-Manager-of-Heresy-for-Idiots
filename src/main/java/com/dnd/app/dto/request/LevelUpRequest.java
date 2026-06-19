package com.dnd.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LevelUpRequest {

    @NotNull(message = "ID класса обязателен")
    private UUID classId;

    @Valid
    private List<RewardSelection> selections;

    // Распределение очков ASI; обязателен, если уровень класса даёт ABILITY_SCORE_IMPROVEMENT.
    @Valid
    private AbilityScoreImprovement abilityScoreImprovement;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardSelection {
        @NotNull(message = "Тип награды обязателен")
        private String rewardType;

        @NotNull(message = "ID записи награды обязателен")
        private UUID rewardEntryId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AbilityScoreImprovement {
        @NotNull(message = "Список повышений характеристик обязателен")
        @Size(min = 1, max = 2, message = "Можно повысить одну или две характеристики")
        @Valid
        private List<StatIncrease> increases;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatIncrease {
        @NotNull(message = "ID характеристики обязателен")
        private UUID statTypeId;

        @NotNull(message = "Размер повышения обязателен")
        @Min(value = 1, message = "Повышение не меньше 1")
        @Max(value = 2, message = "Повышение не больше 2")
        private Integer amount;
    }
}
