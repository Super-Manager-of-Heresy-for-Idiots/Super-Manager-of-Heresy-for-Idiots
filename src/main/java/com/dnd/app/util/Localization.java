package com.dnd.app.util;

/**
 * Класс Localization описывает утилиту, которая поддерживает повторяемые операции бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public final class Localization {

    public static final String RU = "ru";
    public static final String EN = "en";
    public static final String DEFAULT_LANG = EN;

    private Localization() {
    }

    /**
     * Выполняет операции "normalize" в рамках бизнес-логики приложения.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static String normalize(String lang) {
        return RU.equalsIgnoreCase(lang) ? RU : EN;
    }

    /**
     * Выполняет операции "pick" в рамках бизнес-логики приложения.
     * @param lang входящее значение lang, используемое бизнес-сценарием
     * @param rusloc входящее значение rusloc, используемое бизнес-сценарием
     * @param engloc входящее значение engloc, используемое бизнес-сценарием
     * @param canonical входящее значение canonical, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
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
