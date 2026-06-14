package com.dnd.app.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GM override of the battle's total XP. A null value clears the override and reverts to the
 * computed sum of monster base XP.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateBattleXpRequest {

    @Min(value = 0, message = "XP override cannot be negative")
    private Integer overrideXp;
}
