package com.dnd.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateStatRequest {

    @NotNull(message = "Value is required")
    @Min(value = 1, message = "Stat value must be between 1 and 30")
    @Max(value = 30, message = "Stat value must be between 1 and 30")
    private Integer value;
}
