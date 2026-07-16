package com.dnd.app.service.formula;

import com.dnd.app.exception.BadRequestException;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Нормализация пользовательской дайс-нотации к канонической латинской записи NdM.
 * Русскоязычные мастера привычно пишут кости как «8к6» («кубы») или «8д6»; движок формул
 * принимает только латинский разделитель «d». Утилита заменяет русский разделитель между
 * цифрами на «d», не затрагивая остальной текст формулы (имена переменных латинские,
 * поэтому ложных срабатываний нет).
 * Кроме нормализации утилита несёт sanity-cap'ы на кости (Workbench-аудит P0-2 / HB_UX Фаза 2):
 * количество и грани ограничены реальными костями, а длина формулы — защитой от DoS-выражений.
 */
public final class DiceNotation {

    /** Русский разделитель костей («к»/«д») между двумя цифрами, с необязательными пробелами вокруг. */
    private static final Pattern RU_SEPARATOR = Pattern.compile("(?iu)(?<=\\d)\\s*[кд]\\s*(?=\\d)");

    /** Один дайс-токен NdM внутри произвольной формулы (после нормализации к латинскому «d»). */
    private static final Pattern DICE_TOKEN = Pattern.compile("(?i)(\\d+)\\s*d\\s*(\\d+)");

    /** Реальные игровые кости: только эти грани допустимы у homebrew-костей. */
    private static final Set<Integer> ALLOWED_SIDES = Set.of(2, 4, 6, 8, 10, 12, 20, 100);

    /** Верхняя граница количества костей в одном токене (защита от «1000d1000»). */
    private static final int MAX_DICE_COUNT = 40;

    /** Верхняя граница длины дайс-несущей формулы (защита от разрастания выражения). */
    private static final int MAX_FORMULA_LENGTH = 200;

    private DiceNotation() {
    }

    /**
     * Приводит дайс-токены строки к канонической записи: «8к6» / «8 Д 6» → «8d6».
     * Безопасна для полных формул («2к8 + wis_mod» → «2d8 + wis_mod»).
     * @param input исходная строка (допускается {@code null})
     * @return нормализованная строка либо {@code null}, если вход {@code null}
     */
    public static String normalize(String input) {
        if (input == null) {
            return null;
        }
        return RU_SEPARATOR.matcher(input).replaceAll("d");
    }

    /**
     * Проверяет sanity-cap'ы дайс-несущей формулы: длину выражения и каждый токен NdM
     * (количество ≤ 40, грани ∈ {2,4,6,8,10,12,20,100}). Вход ожидается уже нормализованным
     * ({@link #normalize}). Пустой/безкостёвый вход проходит без ошибок (капы не для скаляров).
     * @param normalizedFormula нормализованная формула (латинский разделитель «d»); допускается {@code null}
     * @throws BadRequestException если формула слишком длинная либо токен выходит за реальные кости
     */
    public static void enforceDiceCaps(String normalizedFormula) {
        if (normalizedFormula == null || normalizedFormula.isBlank()) {
            return;
        }
        if (normalizedFormula.length() > MAX_FORMULA_LENGTH) {
            throw new BadRequestException("Формула слишком длинная (максимум " + MAX_FORMULA_LENGTH + " символов)");
        }
        Matcher m = DICE_TOKEN.matcher(normalizedFormula);
        while (m.find()) {
            long count;
            long sides;
            try {
                count = Long.parseLong(m.group(1));
                sides = Long.parseLong(m.group(2));
            } catch (NumberFormatException overflow) {
                throw new BadRequestException("Недопустимые кости: слишком большие числа в «" + m.group() + "»");
            }
            if (count < 1 || count > MAX_DICE_COUNT) {
                throw new BadRequestException(
                        "Слишком много костей: " + count + " (допустимо от 1 до " + MAX_DICE_COUNT + ")");
            }
            if (!ALLOWED_SIDES.contains((int) sides)) {
                throw new BadRequestException(
                        "Недопустимая кость d" + sides + " — используйте d4, d6, d8, d10, d12, d20 или d100");
            }
        }
    }
}
