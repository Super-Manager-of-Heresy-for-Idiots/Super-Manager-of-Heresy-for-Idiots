package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/** Declared result type of a {@code feature_formula}. */
public enum FormulaResultType {

    INTEGER("integer"),
    DECIMAL("decimal"),
    BOOLEAN("boolean"),
    DURATION("duration"),
    DICE("dice"),
    MODIFIER("modifier");

    private final String code;

    FormulaResultType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** True for result types that evaluate to a number (int/decimal/duration/modifier). */
    public boolean isNumeric() {
        return this == INTEGER || this == DECIMAL || this == DURATION || this == MODIFIER;
    }

    public static Optional<FormulaResultType> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(v -> v.code.equalsIgnoreCase(code)).findFirst();
    }
}
