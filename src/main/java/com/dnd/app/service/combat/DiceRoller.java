package com.dnd.app.service.combat;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Thin, injectable wrapper over RNG so combat rolls can be stubbed in tests.
 */
@Component
public class DiceRoller {

    /** Uniformly rolls a d20 (1–20 inclusive). */
    public int rollD20() {
        return ThreadLocalRandom.current().nextInt(1, 21);
    }

    /** Rolls a single die with the given number of sides (1..sides); non-positive sides → 0. */
    public int rollDie(int sides) {
        if (sides < 1) {
            return 0;
        }
        return ThreadLocalRandom.current().nextInt(1, sides + 1);
    }

    /**
     * Rolls a damage expression such as "2d6+3", "1d8" or a flat "5". On a critical hit the dice
     * count is doubled (the flat modifier is not). Unparseable/empty expressions yield 0 and the
     * result is floored at 0.
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
