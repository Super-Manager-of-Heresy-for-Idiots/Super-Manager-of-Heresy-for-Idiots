package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One hit-dice pool of a given die size. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HitDiceResponse {
    private int die;
    private int total;
    private int remaining;
}
