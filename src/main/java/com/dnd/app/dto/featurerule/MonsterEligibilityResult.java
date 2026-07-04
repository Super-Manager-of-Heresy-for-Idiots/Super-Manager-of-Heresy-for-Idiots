package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Whether a monster is an eligible form for a feature, and why not if ineligible. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonsterEligibilityResult {
    private UUID monsterId;
    private boolean eligible;
    private String reason;
}
