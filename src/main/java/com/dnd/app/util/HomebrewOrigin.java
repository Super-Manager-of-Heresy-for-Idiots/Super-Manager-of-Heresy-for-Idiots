package com.dnd.app.util;

import com.dnd.app.domain.HomebrewPackage;

import java.util.UUID;

/**
 * Утилита HomebrewOrigin вычисляет маркер происхождения контента (P0-4 / SEC-4).
 * Единый источник значений source/homebrewId/homebrewTitle для контентных DTO, чтобы клиент
 * мог отличать ванильный (GLOBAL) контент от homebrew без догадок по косвенным полям.
 */
public final class HomebrewOrigin {

    /** Маркер ванильного (системного) контента. */
    public static final String GLOBAL = "GLOBAL";
    /** Маркер homebrew-контента. */
    public static final String HOMEBREW = "HOMEBREW";

    private HomebrewOrigin() {
    }

    /**
     * Возвращает маркер происхождения по homebrew-пакету.
     * @param pkg homebrew-пакет контента (null для ванильного)
     * @return "HOMEBREW", если пакет задан, иначе "GLOBAL"
     */
    public static String source(HomebrewPackage pkg) {
        return pkg != null ? HOMEBREW : GLOBAL;
    }

    /**
     * Возвращает id homebrew-пакета контента.
     * @param pkg homebrew-пакет (null для ванильного)
     * @return id пакета или null
     */
    public static UUID id(HomebrewPackage pkg) {
        return pkg != null ? pkg.getId() : null;
    }

    /**
     * Возвращает заголовок homebrew-пакета контента.
     * @param pkg homebrew-пакет (null для ванильного)
     * @return заголовок пакета или null
     */
    public static String title(HomebrewPackage pkg) {
        return pkg != null ? pkg.getTitle() : null;
    }
}
