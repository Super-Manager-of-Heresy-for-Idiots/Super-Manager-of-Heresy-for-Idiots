package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Result of applying a feature's combat outcome to a target character. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeatureApplyResult {
    private UUID targetCharacterId;
    private Integer damageApplied;
    private Integer healingApplied;
    private Integer targetCurrentHp;
    private Integer targetMaxHp;
}
