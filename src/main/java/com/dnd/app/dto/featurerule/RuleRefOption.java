package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/** Generic reference-table option for admin dropdowns. {@code id} is null for code-only enums (e.g. stacking policy). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleRefOption {
    private UUID id;
    private String code;
    private String label;
}
