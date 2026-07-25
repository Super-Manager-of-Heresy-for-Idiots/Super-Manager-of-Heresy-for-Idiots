package com.dnd.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Класс CreateCampRequest описывает данные разбивки лагеря: локация, состав участников
 * и первичная расстановка дозора. Привал создаётся в статусе SETTING_UP.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampRequest {

    @NotBlank(message = "Camp name is required")
    @Size(max = 120, message = "Camp name must not exceed 120 characters")
    private String name;

    private String description;

    @Min(value = 1, message = "Day number must be positive")
    private Integer dayNumber;

    /** Локация привала; null — отряд встал в пути. */
    private UUID locationId;

    /** Персонажи кампании, входящие в привал. */
    private List<UUID> participantCharacterIds;

    @Min(value = 0, message = "Watch slot count must not be negative")
    @Max(value = 12, message = "Watch slot count must not exceed 12")
    private Integer watchSlotCount;

    /** Первичное расписание дозора; можно переставить позже. */
    @Valid
    private List<CampWatchSlotRequest> watchSchedule;
}
