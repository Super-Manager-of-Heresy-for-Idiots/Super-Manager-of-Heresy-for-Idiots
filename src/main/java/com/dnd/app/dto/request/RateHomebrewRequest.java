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
public class RateHomebrewRequest {

    @NotNull(message = "Rating is required")
    @Min(value = -1, message = "Rating must be at least -1")
    @Max(value = 1, message = "Rating must be at most 1")
    private Integer rating;
}
