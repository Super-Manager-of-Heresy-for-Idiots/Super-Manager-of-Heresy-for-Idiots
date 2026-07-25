package com.dnd.app.dto.request;

import com.dnd.app.domain.enums.RollPromptType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс CreateRollPromptRequest описывает DTO запроса мастера "запросить проверку"
 * (ROLL_PROMPT): целевые персонажи, тип проверки, характеристика, Сложность и режим броска.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRollPromptRequest {

    /** Персонажи, у владельцев которых появится окно броска. */
    @NotEmpty(message = "characterIds must not be empty")
    private List<UUID> characterIds;

    @NotNull(message = "rollType is required")
    private RollPromptType rollType;

    /** Обязателен для ABILITY_CHECK и SAVING_THROW. */
    private UUID statTypeId;

    @Min(value = 1, message = "dc must be at least 1")
    @Max(value = 50, message = "dc must be at most 50")
    private Integer dc;

    /** Скрыть DC от игрока до броска. */
    private Boolean hideDc;

    @Pattern(regexp = "NORMAL|ADVANTAGE|DISADVANTAGE", message = "advantageMode must be NORMAL, ADVANTAGE or DISADVANTAGE")
    private String advantageMode;

    @Size(max = 200, message = "description must be at most 200 characters")
    private String description;
}
