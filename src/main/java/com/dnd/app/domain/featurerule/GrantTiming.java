package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/** When a static grant is applied. */
public enum GrantTiming {

    LEVEL_UP("level_up"),
    ALWAYS("always"),
    REST("rest"),
    FEATURE_USE("feature_use");

    private final String code;

    GrantTiming(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Optional<GrantTiming> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(v -> v.code.equalsIgnoreCase(code)).findFirst();
    }
}
