package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/** The kind of entity a choice option / persisted character choice points at (plan §4.12). */
public enum ChoiceOptionType {

    SPELL("spell"),
    SKILL("skill"),
    LANGUAGE("language"),
    PROFICIENCY("proficiency"),
    MONSTER("monster"),
    ITEM("item"),
    FEATURE("feature"),
    DAMAGE_TYPE("damage_type");

    private final String code;

    ChoiceOptionType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Optional<ChoiceOptionType> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(v -> v.code.equalsIgnoreCase(code)).findFirst();
    }
}
