package com.dnd.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A death saving throw. Omit {@code roll} to have the server roll the d20; supply it for a manual roll. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeathSaveRequest {

    @Min(value = 1, message = "roll must be between 1 and 20")
    @Max(value = 20, message = "roll must be between 1 and 20")
    private Integer roll;
}
