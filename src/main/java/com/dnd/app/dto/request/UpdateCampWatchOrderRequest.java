package com.dnd.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Класс UpdateCampWatchOrderRequest описывает полную перестановку дозора: расписание
 * задаётся целиком, персонажи вне переданных слотов остаются без дозора.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCampWatchOrderRequest {

    @Min(value = 0, message = "Watch slot count must not be negative")
    @Max(value = 12, message = "Watch slot count must not exceed 12")
    private Integer watchSlotCount;

    @Valid
    private List<CampWatchSlotRequest> watchSchedule;
}
