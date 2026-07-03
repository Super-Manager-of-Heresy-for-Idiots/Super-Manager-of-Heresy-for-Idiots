package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A game source (sourcebook / source pack) option. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleSourceResponse {
    private UUID id;
    private String key;
    private String displayName;
    private String sourceType;
    private UUID rulesetId;
}
