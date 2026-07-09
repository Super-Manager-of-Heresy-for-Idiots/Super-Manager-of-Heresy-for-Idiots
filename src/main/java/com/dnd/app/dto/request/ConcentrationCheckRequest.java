package com.dnd.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A concentration saving throw the player rolls (Phase 2.2). Omit {@code d20} to have the server roll
 * (AUTO); supply it (1–20) for a manual physical roll. The save bonus (Con) is computed server-side and
 * checked against the pending DC recorded when the character took damage.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConcentrationCheckRequest {

    @Min(value = 1, message = "d20 must be between 1 and 20")
    @Max(value = 20, message = "d20 must be between 1 and 20")
    private Integer d20;
}
