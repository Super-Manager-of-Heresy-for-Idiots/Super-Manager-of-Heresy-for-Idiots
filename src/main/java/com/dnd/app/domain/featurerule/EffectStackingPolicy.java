package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/** How a newly applied effect interacts with existing ones (plan §4.6). */
public enum EffectStackingPolicy {

    STACK("stack"),
    REPLACE_SAME_FEATURE("replace_same_feature"),
    REPLACE_SAME_GROUP("replace_same_group"),
    HIGHEST_ONLY("highest_only");

    private final String code;

    EffectStackingPolicy(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Optional<EffectStackingPolicy> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(v -> v.code.equalsIgnoreCase(code)).findFirst();
    }
}
