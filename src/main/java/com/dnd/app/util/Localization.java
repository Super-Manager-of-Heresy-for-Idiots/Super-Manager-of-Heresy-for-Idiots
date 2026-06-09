package com.dnd.app.util;

/**
 * Picks a user-facing string for the requested UI language.
 *
 * Reference rows store two parallel columns per localized field:
 * {@code *_engloc} (English) and {@code *_rusloc} (Russian). The canonical
 * column ({@code name}/{@code description}/...) is always English and is never
 * null, so it serves as the last-resort fallback for rows that predate the
 * localization seed (their {@code *_engloc}/{@code *_rusloc} are still null).
 */
public final class Localization {

    public static final String RU = "ru";
    public static final String EN = "en";
    public static final String DEFAULT_LANG = EN;

    private Localization() {
    }

    /** Normalizes any incoming value to {@code "ru"} or {@code "en"} (default). */
    public static String normalize(String lang) {
        return RU.equalsIgnoreCase(lang) ? RU : EN;
    }

    /**
     * Returns the requested locale if present; otherwise the other locale;
     * otherwise the canonical (English) value. Never returns null unless the
     * canonical itself is null.
     */
    public static String pick(String lang, String rusloc, String engloc, String canonical) {
        boolean ru = RU.equalsIgnoreCase(lang);
        String primary = ru ? rusloc : engloc;
        String secondary = ru ? engloc : rusloc;
        if (isPresent(primary)) return primary;
        if (isPresent(secondary)) return secondary;
        return canonical;
    }

    private static boolean isPresent(String s) {
        return s != null && !s.isBlank();
    }
}
