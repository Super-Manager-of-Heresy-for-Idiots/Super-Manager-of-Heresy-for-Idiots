package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** The complete formula DSL vocabulary (functions + scalars) served to the admin autocomplete. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormulaVocabularyResponse {
    private List<FormulaVocabEntry> functions;
    private List<FormulaVocabEntry> scalars;
    /** Ability codes usable as {@code ability_mod("…")} args (STR, DEX, …). */
    private List<String> abilityCodes;
}
