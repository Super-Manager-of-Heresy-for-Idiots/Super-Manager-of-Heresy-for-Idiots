package com.dnd.app.service.formula;

import java.util.regex.Pattern;

/**
 * Нормализация пользовательской дайс-нотации к канонической латинской записи NdM.
 * Русскоязычные мастера привычно пишут кости как «8к6» («кубы») или «8д6»; движок формул
 * принимает только латинский разделитель «d». Утилита заменяет русский разделитель между
 * цифрами на «d», не затрагивая остальной текст формулы (имена переменных латинские,
 * поэтому ложных срабатываний нет).
 */
public final class DiceNotation {

    /** Русский разделитель костей («к»/«д») между двумя цифрами, с необязательными пробелами вокруг. */
    private static final Pattern RU_SEPARATOR = Pattern.compile("(?iu)(?<=\\d)\\s*[кд]\\s*(?=\\d)");

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
}
