package com.dnd.app.service.homebrew;

import com.dnd.app.exception.BadRequestException;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * HB_MODES: нормализация и валидация трёх режимов homebrew-сущности (NEW / DERIVED / OVERRIDE).
 * Общая логика для авторинга заклинаний и предметов: OVERRIDE обязан ссылаться на оригинал; оригинал —
 * только ваниль или сущность СВОЕГО пакета (перезапись чужого homebrew запрещена — мораторий безопасности).
 */
public final class HomebrewOriginModes {

    public static final String NEW = "NEW";
    public static final String DERIVED = "DERIVED";
    public static final String OVERRIDE = "OVERRIDE";

    private static final Set<String> ALL = Set.of(NEW, DERIVED, OVERRIDE);

    private HomebrewOriginModes() {
    }

    /** Нормализует режим (null/пусто → NEW); неизвестное значение — 400. */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return NEW;
        }
        String mode = raw.trim().toUpperCase(Locale.ROOT);
        if (!ALL.contains(mode)) {
            throw new BadRequestException("Неизвестный режим homebrew: " + raw + " (ожидается NEW/DERIVED/OVERRIDE)");
        }
        return mode;
    }

    /**
     * Валидирует пару режим+оригинал. Для NEW ссылка игнорируется (возвращается null); для DERIVED ссылка
     * опциональна; для OVERRIDE — обязательна. Если ссылка задана, оригинал обязан существовать и быть
     * ванильным либо принадлежать своему пакету.
     * @param mode нормализованный режим
     * @param sourceId ссылка на оригинал из запроса (может быть null)
     * @param sourceExists найден ли оригинал по ссылке
     * @param sourceHomebrewId пакет оригинала (null = ваниль)
     * @param ownPackageId пакет автора
     * @return ссылка, которую следует сохранить (null для NEW/пустой DERIVED)
     */
    public static UUID validateSource(String mode, UUID sourceId, boolean sourceExists,
                                      UUID sourceHomebrewId, UUID ownPackageId) {
        if (NEW.equals(mode)) {
            return null;
        }
        if (sourceId == null) {
            if (OVERRIDE.equals(mode)) {
                throw new BadRequestException("Режим OVERRIDE требует ссылку на перезаписываемый оригинал (sourceId)");
            }
            return null; // DERIVED без ссылки — допустимо (провенанс не указан)
        }
        if (!sourceExists) {
            throw new BadRequestException("Оригинал для режима " + mode + " не найден: " + sourceId);
        }
        if (sourceHomebrewId != null && !sourceHomebrewId.equals(ownPackageId)) {
            throw new BadRequestException("Оригиналом может быть только ванильная сущность или сущность своего пакета");
        }
        return sourceId;
    }
}
