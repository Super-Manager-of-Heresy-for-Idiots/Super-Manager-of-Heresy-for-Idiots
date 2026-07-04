package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/** Lifecycle status of a character's active feature effect. */
public enum ActiveEffectStatus {

    ACTIVE("active"),
    EXPIRED("expired"),
    ENDED("ended");

    private final String code;

    ActiveEffectStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Optional<ActiveEffectStatus> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(v -> v.code.equalsIgnoreCase(code)).findFirst();
    }
}
