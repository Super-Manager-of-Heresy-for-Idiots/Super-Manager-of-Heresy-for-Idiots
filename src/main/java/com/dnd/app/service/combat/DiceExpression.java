package com.dnd.app.service.combat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Запись DiceExpression описывает сервис боевой логики, который рассчитывает и применяет правила боя.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 * @param count входящее значение count, используемое бизнес-сценарием
 * @param sides входящее значение sides, используемое бизнес-сценарием
 * @param modifier входящее значение modifier, используемое бизнес-сценарием
 */
public record DiceExpression(int count, int sides, int modifier) {

    private static final Pattern DICE = Pattern.compile(
            "\\s*(\\d*)\\s*[dDкКkK]\\s*(\\d+)\\s*([+\\-]\\s*\\d+)?\\s*");
    private static final Pattern FLAT = Pattern.compile("\\s*([+\\-]?\\d+)\\s*");

    /**
     * Верхняя граница количества костей и числа граней. Легальные D&D-выражения на порядки меньше;
     * ограничение защищает от переполнения и раскрутки цикла броска (DoS), если во входных данных
     * окажется абсурдно большое значение.
     */
    private static final int MAX_DICE = 1000;

    /** Безопасно разбирает число костей/граней, обрезая слишком большие значения до {@link #MAX_DICE}. */
    private static int clampDice(String digits) {
        if (digits.length() > 6) {
            return MAX_DICE;
        }
        return Math.min(Integer.parseInt(digits), MAX_DICE);
    }

    /**
     * Выполняет операции "parse" в рамках бизнес-логики боя.
     * @param expr входящее значение expr, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static DiceExpression parse(String expr) {
        if (expr == null || expr.isBlank()) {
            return null;
        }
        Matcher m = DICE.matcher(expr);
        if (m.matches()) {
            int count = m.group(1).isEmpty() ? 1 : clampDice(m.group(1));
            int sides = clampDice(m.group(2));
            int mod = (m.group(3) == null) ? 0 : Integer.parseInt(m.group(3).replaceAll("\\s", ""));
            return new DiceExpression(count, sides, mod);
        }
        Matcher f = FLAT.matcher(expr);
        if (f.matches()) {
            return new DiceExpression(0, 0, Integer.parseInt(f.group(1)));
        }
        return null;
    }

    /**
     * Выполняет операции "min" в рамках бизнес-логики боя.
     * @return результат выполнения бизнес-операции
     */
    public int min() {
        return count + modifier;
    }

    /**
     * Выполняет операции "max" в рамках бизнес-логики боя.
     * @return результат выполнения бизнес-операции
     */
    public int max() {
        return count * sides + modifier;
    }
}
