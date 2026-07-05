package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** A rule-target option (self, ally, creature, …) for the admin editor dropdown. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TargetTypeOption {
    private UUID id;
    private String code;
    private String label;
}
