package com.dnd.app.service.combat;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Класс DiceRoller описывает сервис боевой логики, который рассчитывает и применяет правила боя.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Component
public class DiceRoller {

    /**
     * Выполняет бросок операции "roll d20" в рамках бизнес-логики боя.
     * @return результат выполнения бизнес-операции
     */
    public int rollD20() {
        return ThreadLocalRandom.current().nextInt(1, 21);
    }

    /**
     * Выполняет бросок операции "roll die" в рамках бизнес-логики боя.
     * @param sides входящее значение sides, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public int rollDie(int sides) {
        if (sides < 1) {
            return 0;
        }
        return ThreadLocalRandom.current().nextInt(1, sides + 1);
    }

    /**
     * Выполняет бросок операции "roll damage" в рамках бизнес-логики боя.
     * @param expression входящее значение expression, используемое бизнес-сценарием
     * @param critical входящее значение critical, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public int rollDamage(String expression, boolean critical) {
        DiceExpression dice = DiceExpression.parse(expression);
        if (dice == null) {
            return 0;
        }
        int diceCount = critical ? dice.count() * 2 : dice.count();
        int sum = dice.modifier();
        for (int i = 0; i < diceCount; i++) {
            sum += rollDie(dice.sides());
        }
        return Math.max(0, sum);
    }
}
