package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Result of spending hit dice on a short rest: how much was healed and the character's HP after. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HitDiceSpendResponse {
    private int die;
    private int remaining;
    private int healed;
    private int currentHp;
    private int tempHp;
    private int maxHp;
}
