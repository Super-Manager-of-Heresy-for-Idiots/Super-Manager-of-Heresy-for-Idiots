package com.dnd.app.service.formula;

/** A dice expression result, e.g. {@code 2d6}. */
public record DiceValue(int count, int sides) {

    public String toExpression() {
        return count + "d" + sides;
    }

    /** Average roll value, used when a dice formula must collapse to a number. */
    public double average() {
        return count * (sides + 1) / 2.0;
    }
}
