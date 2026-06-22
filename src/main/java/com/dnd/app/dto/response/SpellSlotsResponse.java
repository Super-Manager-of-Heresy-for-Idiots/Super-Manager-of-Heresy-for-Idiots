package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Spell slots view for a character: per spell level the derived maximum, how many are
 * expended, and how many remain available.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SpellSlotsResponse {

    private List<SlotLevel> levels;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlotLevel {
        private int spellLevel;
        private int max;
        private int expended;
        private int available;
    }
}
