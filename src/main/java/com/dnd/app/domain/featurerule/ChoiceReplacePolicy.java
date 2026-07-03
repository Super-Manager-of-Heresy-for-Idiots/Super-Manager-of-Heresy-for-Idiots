package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/** Whether/when previously made choices can be replaced (plan §4.12). */
public enum ChoiceReplacePolicy {

    NEVER("never"),
    ON_LEVEL_UP("on_level_up"),
    ON_REST("on_rest"),
    ON_RULE_TEXT("on_rule_text");

    private final String code;

    ChoiceReplacePolicy(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Optional<ChoiceReplacePolicy> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(v -> v.code.equalsIgnoreCase(code)).findFirst();
    }
}
