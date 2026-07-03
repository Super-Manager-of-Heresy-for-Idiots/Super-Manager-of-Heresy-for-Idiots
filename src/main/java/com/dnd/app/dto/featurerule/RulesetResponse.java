package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A game ruleset / edition option. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RulesetResponse {
    private UUID id;
    private String key;
    private String displayName;
    private String edition;
    private boolean enabled;
}
