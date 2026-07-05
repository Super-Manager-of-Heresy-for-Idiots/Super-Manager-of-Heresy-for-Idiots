package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Reference vocabularies for the SAVE_CHECK_ATTACK editor (resolution types, abilities, skills). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolutionMetadataResponse {
    private List<RuleRefOption> resolutionTypes;
    private List<RuleRefOption> abilities;
    private List<RuleRefOption> skills;
}
