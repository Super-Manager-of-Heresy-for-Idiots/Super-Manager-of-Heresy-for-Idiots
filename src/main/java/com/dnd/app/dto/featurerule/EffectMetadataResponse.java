package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Reference vocabularies for the ACTIVE_EFFECT editor (duration units, stacking policies, targets, end triggers). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EffectMetadataResponse {
    private List<RuleRefOption> durationUnits;
    private List<RuleRefOption> stackingPolicies;
    private List<RuleRefOption> targetTypes;
    private List<RuleRefOption> restTypes;
    private List<RuleRefOption> triggerEventTypes;
}
