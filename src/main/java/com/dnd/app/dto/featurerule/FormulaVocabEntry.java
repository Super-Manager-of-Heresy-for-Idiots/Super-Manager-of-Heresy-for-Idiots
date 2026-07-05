package com.dnd.app.dto.featurerule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One entry in the formula DSL vocabulary (a function or a scalar) for the admin autocomplete. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormulaVocabEntry {
    /** Bare token name (e.g. {@code ability_mod}, {@code character_level}). */
    private String name;
    /** {@code scalar} | {@code function}. */
    private String kind;
    /** Text to insert on selection (e.g. {@code ability_mod(} for a function, {@code character_level} for a scalar). */
    private String insertText;
    /** Human-facing signature hint (e.g. {@code ability_mod("STR")}). */
    private String signature;
    /**
     * For keyed functions, which dynamic vocabulary fills the string argument:
     * {@code ability} | {@code class} | {@code resource_key} | {@code target} | {@code dice} | null.
     */
    private String argKind;
    /** Localized one-line description. */
    private String description;
}
