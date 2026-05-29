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
public class SkillEffectRequest {

    @NotNull(message = "buffDebuffId обязателен")
    private UUID buffDebuffId;

    @NotBlank(message = "effectRole обязателен")
    private String effectRole;

    @NotNull(message = "chancePercent обязателен")
    @Min(value = 1, message = "chancePercent должен быть от 1 до 100")
    @Max(value = 100, message = "chancePercent должен быть от 1 до 100")
    private Integer chancePercent;
}
