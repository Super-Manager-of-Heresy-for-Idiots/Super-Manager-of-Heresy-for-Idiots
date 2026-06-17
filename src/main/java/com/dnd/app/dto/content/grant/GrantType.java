package com.dnd.app.dto.content.grant;

/**
 * Known reward grant types. The DB column {@code grant_type} is flexible text:
 * the app recognizes these known values and renders anything else as
 * {@link #CUSTOM_TEXT} (custom/manual).
 */
public enum GrantType {
    FEATURE,
    SUBCLASS,
    FEAT,
    SPELL,
    SKILL_PROFICIENCY,
    ABILITY_SCORE,
    NUMERIC_MODIFIER,
    CUSTOM_TEXT;

    /** Resolve a free-text grant type to a known value, defaulting to CUSTOM_TEXT. */
    public static GrantType fromTextOrCustom(String raw) {
        if (raw == null) {
            return CUSTOM_TEXT;
        }
        for (GrantType type : values()) {
            if (type.name().equalsIgnoreCase(raw.trim())) {
                return type;
            }
        }
        return CUSTOM_TEXT;
    }
}
