package com.dnd.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class CreateHomebrewClassRequest {

    @NotBlank(message = "Название обязательно")
    @Size(max = 50, message = "Название не должно превышать 50 символов")
    private String name;

    private String description;

    @Valid
    private List<LevelPlan> levels;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelPlan {

        @NotNull(message = "Уровень обязателен")
        @Min(value = 1, message = "Уровень должен быть от 1 до 20")
        @Max(value = 20, message = "Уровень должен быть от 1 до 20")
        private Integer level;

        @Valid
        private List<RewardPlan> rewards;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RewardPlan {

        @NotBlank(message = "Тип награды обязателен")
        private String rewardType;

        private UUID rewardId;

        private Boolean isChoice;

        @Valid
        private InlineSkillRequest skill;

        @Valid
        private CreateFeatRequest feat;

        @Valid
        private CreateBuffDebuffRequest buffDebuff;

        @Valid
        private InlineSubclassRequest subclass;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InlineSkillRequest {

        @NotBlank(message = "Название умения обязательно")
        @Size(max = 100, message = "Название не должно превышать 100 символов")
        private String name;

        private String description;

        @Size(max = 50, message = "Тип умения не должен превышать 50 символов")
        private String skillType;

        @Pattern(regexp = "^(\\d+)?d(\\d+)$", message = "Формат должен быть NdM или dM")
        private String damageDice;

        private Integer damageBonus;

        private String damageType;

        @Valid
        private List<InlineSkillEffectRequest> effects;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InlineSkillEffectRequest {

        private UUID buffDebuffId;

        @Valid
        private CreateBuffDebuffRequest buffDebuff;

        @NotBlank(message = "Роль эффекта обязательна")
        private String effectRole;

        @NotNull(message = "Шанс эффекта обязателен")
        @Min(value = 1, message = "Шанс должен быть от 1 до 100")
        @Max(value = 100, message = "Шанс должен быть от 1 до 100")
        private Integer chancePercent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InlineSubclassRequest {

        @NotBlank(message = "Название подкласса обязательно")
        @Size(max = 100, message = "Название не должно превышать 100 символов")
        private String name;

        private String description;
    }
}
