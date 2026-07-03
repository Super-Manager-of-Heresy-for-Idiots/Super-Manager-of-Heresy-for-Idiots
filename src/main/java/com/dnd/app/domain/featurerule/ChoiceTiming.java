package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/** When a choice group becomes available to resolve (plan §4.12). */
public enum ChoiceTiming {

    LEVEL_UP("level_up"),
    LONG_REST("long_rest"),
    SHORT_REST("short_rest"),
    ALWAYS_AVAILABLE("always_available"),
    MANUAL_ADMIN("manual_admin");

    private final String code;

    ChoiceTiming(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Optional<ChoiceTiming> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(v -> v.code.equalsIgnoreCase(code)).findFirst();
    }
}
