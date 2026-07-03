package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** One resource's before/after value for a rest preview or completion. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestResourcePreview {
    private UUID resourceId;
    private String resourceKey;
    private String displayName;
    private Integer currentValue;
    private Integer willBeValue;
}
