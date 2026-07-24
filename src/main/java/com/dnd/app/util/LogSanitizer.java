package com.dnd.app.util;

/**
 * Утилита LogSanitizer обезвреживает пользовательские значения перед записью в лог.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 *
 * <p>Назначение — защита от log injection / log forging: злоумышленник может передать
 * во входных данных (имя пользователя, заголовки Referer/Origin/User-Agent и т.п.) символы
 * перевода строки (CR/LF) и управляющие символы, чтобы подделать отдельную строку лога или
 * внедрить управляющие последовательности терминала. Метод {@link #clean(String)} заменяет
 * все ISO-control-символы на '_' и ограничивает длину значения.</p>
 */
public final class LogSanitizer {

    /** Максимальная длина логируемого значения — обрезаем, чтобы длинный ввод не раздувал логи. */
    private static final int MAX_LENGTH = 256;

    private LogSanitizer() {
    }

    /**
     * Обезвреживает значение для безопасной записи в лог: убирает CR/LF и прочие управляющие
     * символы, обрезает по длине. Null-безопасен.
     * @param value входящее значение, потенциально контролируемое пользователем
     * @return безопасная для логирования строка (или null, если на входе null)
     */
    public static String clean(String value) {
        if (value == null) {
            return null;
        }
        String bounded = value.length() > MAX_LENGTH ? value.substring(0, MAX_LENGTH) + "…" : value;
        StringBuilder sb = new StringBuilder(bounded.length());
        for (int i = 0; i < bounded.length(); i++) {
            char c = bounded.charAt(i);
            sb.append(Character.isISOControl(c) ? '_' : c);
        }
        return sb.toString();
    }
}
