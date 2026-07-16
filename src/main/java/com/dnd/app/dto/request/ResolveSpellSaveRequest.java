package com.dnd.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO ResolveSpellSaveRequest — выбор ответственного за цель при разрешении отложенного исхода заклинания
 * (SAVE_PROMPT). {@code outcome} авторитетен (движок применяет ровно его); {@code d20} необязателен и служит
 * лишь для лога/рекомендации, если игрок кинул спасбросок сам.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveSpellSaveRequest {

    /** Выбор исхода игроком: FULL (весь урон) | HALF (половина) | NONE (не получать). */
    @NotBlank(message = "Outcome is required")
    @Pattern(regexp = "FULL|HALF|NONE", message = "Outcome must be FULL, HALF or NONE")
    private String outcome;

    /** Необязательный собственный бросок спасброска игрока (1–20) — только для лога/рекомендации. */
    @Min(value = 1, message = "d20 must be between 1 and 20")
    @Max(value = 20, message = "d20 must be between 1 and 20")
    private Integer d20;
}
