package com.dnd.app.service.homebrew;

import com.dnd.app.dto.request.HomebrewSpellRequest;
import com.dnd.app.exception.BadRequestException;

import java.util.Locale;
import java.util.Set;

/**
 * Генерация отображаемых строк заклинания из СТРУКТУРНЫХ полей и валидация слагов пикеров (HB_UX Фаза 1/3).
 * Принцип плана: приложение никогда не парсит свободную форму — пользователь выбирает из словарей, механика
 * пишется структурно, а строка карточки («1 действие», «Концентрация, до 1 минуты») ГЕНЕРИРУЕТСЯ из структуры.
 * Расхождение «что видит игрок» vs «что исполняет движок» становится невозможным по построению.
 * Все слаги совпадают со словарём ванильного импорта (DndContentLoader) и тем, что читает боевой движок.
 */
public final class SpellDisplayStrings {

    /** Экономика действия каста (совпадает с casting_action_slug ванильного импорта). */
    public static final Set<String> CASTING_ACTIONS = Set.of("action", "bonus-action", "reaction", "time");
    /** Единицы «долгого» каста. */
    public static final Set<String> CASTING_TIME_UNITS = Set.of("minute", "hour");
    /** Типы дистанции (self/touch/distance читает range-гейтинг; sight/unlimited — без гейта). */
    public static final Set<String> RANGE_TYPES = Set.of("self", "touch", "distance", "sight", "unlimited");
    /** Единица дистанции (движок надёжно понимает только футы). */
    public static final Set<String> RANGE_UNITS = Set.of("ft");
    /** Типы длительности. */
    public static final Set<String> DURATION_TYPES = Set.of("instantaneous", "timed", "until-dispelled", "special");
    /** Единицы длительности (конвертируются в раунды детерминированно). */
    public static final Set<String> DURATION_UNITS = Set.of("round", "minute", "hour", "day");
    /** Формы области действия (миграция 091). */
    public static final Set<String> AREA_SHAPES = Set.of("SPHERE", "CUBE", "CONE", "CYLINDER", "LINE");
    /** Труднопроходимость зоны. */
    public static final Set<String> ZONE_TERRAINS = Set.of("DIFFICULT");
    /** Затруднение видимости в зоне. */
    public static final Set<String> ZONE_OBSCUREMENTS = Set.of("LIGHT", "HEAVY");

    private SpellDisplayStrings() {
    }

    /**
     * Проверяет, что слаг входит в допустимый словарь (null/пусто — допустимо, означает «не задано»).
     * @param value значение слага из запроса
     * @param allowed словарь допустимых слагов
     * @param field человекочитаемое имя поля для сообщения об ошибке
     * @throws BadRequestException если значение непусто и не входит в словарь
     */
    public static void validateSlug(String value, Set<String> allowed, String field) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (!allowed.contains(value)) {
            throw new BadRequestException("Недопустимое значение поля «" + field + "»: " + value);
        }
    }

    /**
     * Валидирует все структурные слаги запроса заклинания разом (перед записью в сущность).
     * @param request тело запроса
     */
    public static void validate(HomebrewSpellRequest request) {
        validateSlug(request.getCastingActionSlug(), CASTING_ACTIONS, "время сотворения");
        validateSlug(request.getCastingTimeUnit(), CASTING_TIME_UNITS, "единица времени каста");
        validateSlug(request.getRangeType(), RANGE_TYPES, "дистанция");
        validateSlug(request.getRangeUnit(), RANGE_UNITS, "единица дистанции");
        validateSlug(request.getDurationType(), DURATION_TYPES, "тип длительности");
        validateSlug(request.getDurationUnit(), DURATION_UNITS, "единица длительности");
        validateSlug(request.getAreaShape(), AREA_SHAPES, "форма области");
        validateSlug(request.getZoneTerrain(), ZONE_TERRAINS, "местность зоны");
        validateSlug(request.getZoneObscurement(), ZONE_OBSCUREMENTS, "затруднение зоны");
        if ("time".equals(request.getCastingActionSlug())
                && (request.getCastingTimeAmount() == null || request.getCastingTimeUnit() == null)) {
            throw new BadRequestException("Для «долгого» каста укажите количество и единицу времени");
        }
        if ("distance".equals(request.getRangeType()) && request.getRangeDistance() == null) {
            throw new BadRequestException("Для дистанции «N футов» укажите значение дистанции");
        }
        if ("timed".equals(request.getDurationType())
                && (request.getDurationAmount() == null || request.getDurationUnit() == null)) {
            throw new BadRequestException("Для конечной длительности укажите количество и единицу");
        }
    }

    /**
     * Генерирует строку «Время сотворения» из casting_action_slug (+ amount/unit для долгого каста).
     * @param request тело запроса
     * @return отображаемая строка либо {@code null}, если экономика действия не задана
     */
    public static String castingTimeRaw(HomebrewSpellRequest request) {
        String slug = request.getCastingActionSlug();
        if (slug == null || slug.isBlank()) {
            return null;
        }
        return switch (slug) {
            case "action" -> "1 действие";
            case "bonus-action" -> "1 бонусное действие";
            case "reaction" -> "1 реакция";
            case "time" -> ruAmount(request.getCastingTimeAmount(), request.getCastingTimeUnit());
            default -> null;
        };
    }

    /**
     * Генерирует строку «Длительность» из duration_type (+ amount/unit + концентрация).
     * @param request тело запроса
     * @return отображаемая строка либо {@code null}, если тип длительности не задан
     */
    public static String durationRaw(HomebrewSpellRequest request) {
        String type = request.getDurationType();
        boolean conc = Boolean.TRUE.equals(request.getConcentration());
        if (type == null || type.isBlank()) {
            return conc ? "Концентрация" : null;
        }
        return switch (type) {
            case "instantaneous" -> "Мгновенная";
            case "until-dispelled" -> "Пока не рассеется";
            case "special" -> "Особая";
            case "timed" -> {
                String amount = ruAmount(request.getDurationAmount(), request.getDurationUnit());
                yield conc ? "Концентрация, до " + amount : amount;
            }
            default -> null;
        };
    }

    /**
     * Форматирует «число + единица» по-русски с правильным склонением (round/minute/hour/day).
     * @param amount количество
     * @param unit единица
     * @return строка вида «10 минут», «1 час»; {@code null}, если данных нет
     */
    public static String ruAmount(Integer amount, String unit) {
        if (amount == null || unit == null) {
            return null;
        }
        String word = switch (unit.toLowerCase(Locale.ROOT)) {
            case "round" -> ruPlural(amount, "раунд", "раунда", "раундов");
            case "minute" -> ruPlural(amount, "минута", "минуты", "минут");
            case "hour" -> ruPlural(amount, "час", "часа", "часов");
            case "day" -> ruPlural(amount, "день", "дня", "дней");
            default -> unit;
        };
        return amount + " " + word;
    }

    /** Русское склонение по числу: form1 (1), form2 (2–4), form5 (0, 5–20). */
    private static String ruPlural(int n, String form1, String form2, String form5) {
        int mod100 = Math.abs(n) % 100;
        int mod10 = mod100 % 10;
        if (mod100 >= 11 && mod100 <= 14) {
            return form5;
        }
        if (mod10 == 1) {
            return form1;
        }
        if (mod10 >= 2 && mod10 <= 4) {
            return form2;
        }
        return form5;
    }
}
