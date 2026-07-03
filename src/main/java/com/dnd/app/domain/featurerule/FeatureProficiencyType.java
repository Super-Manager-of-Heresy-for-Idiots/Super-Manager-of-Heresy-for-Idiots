package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/** Kind of proficiency a {@code feature_proficiency_grant} confers (plan §4.15). */
public enum FeatureProficiencyType {

    SKILL("skill"),
    WEAPON("weapon"),
    ARMOR("armor"),
    TOOL("tool"),
    SAVING_THROW("saving_throw");

    private final String code;

    FeatureProficiencyType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static Optional<FeatureProficiencyType> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(v -> v.code.equalsIgnoreCase(code)).findFirst();
    }
}
