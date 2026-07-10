package com.dnd.app.util;

import java.security.SecureRandom;

/**
 * Класс InviteCodeGenerator описывает утилиту, которая поддерживает повторяемые операции бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public final class InviteCodeGenerator {

    private static final String CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 8;

    private InviteCodeGenerator() {}

    /**
     * Выполняет операции "generate" в рамках бизнес-логики приложения.
     * @return результат выполнения бизнес-операции
     */
    public static String generate() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}
