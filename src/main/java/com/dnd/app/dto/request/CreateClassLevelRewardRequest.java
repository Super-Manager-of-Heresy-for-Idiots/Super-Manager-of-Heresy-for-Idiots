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

    @NotNull(message = "Требуемый уровень обязателен")
    @Min(value = 1, message = "Требуемый уровень должен быть от 1 до 20")
    @Max(value = 20, message = "Требуемый уровень должен быть от 1 до 20")
    private Integer requiredLevel;

    @NotBlank(message = "Тип награды обязателен")
    private String rewardType;

    @NotNull(message = "ID награды обязателен")
    private UUID rewardId;

    private Boolean isChoice;
}
