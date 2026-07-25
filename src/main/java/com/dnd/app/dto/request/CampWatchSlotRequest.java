package com.dnd.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс CampWatchSlotRequest описывает один слот расписания дозора: номер, метку времени
 * и назначенного персонажа. Пустой слот приходит без characterId.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampWatchSlotRequest {

    @NotNull(message = "Watch slot number is required")
    @Min(value = 1, message = "Watch slot number must be positive")
    private Integer slot;

    @Size(max = 40, message = "Watch slot label must not exceed 40 characters")
    private String label;

    private UUID characterId;
}
