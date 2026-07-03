package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Generic {@code {code, label}} option used to drive admin dropdowns/filters. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeLabel {
    private String code;
    private String label;
}
