package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/** How a numeric formula result is rounded before {@code min}/{@code max} clamping. */
public enum FormulaRoundingMode {

    FLOOR("floor"),
    CEIL("ceil"),
    NEAREST("nearest"),
    NONE("none");

    private final String code;

    FormulaRoundingMode(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public double apply(double value) {
        return switch (this) {
            case FLOOR -> Math.floor(value);
            case CEIL -> Math.ceil(value);
            case NEAREST -> Math.rint(value);
            case NONE -> value;
        };
    }

    public static Optional<FormulaRoundingMode> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(v -> v.code.equalsIgnoreCase(code)).findFirst();
    }
}
