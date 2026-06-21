package com.dnd.app.service.combat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DiceExpression / DiceRoller: разбор и бросок выражений урона")
class DiceRollerTest {

    private final DiceRoller diceRoller = new DiceRoller();

    // ------------------------------ Разбор ------------------------------

    @Test
    @DisplayName("Разбор форм '2d6+3', '1d8', 'd20' и '1к8' (кириллический маркер)")
    void parse_supportsCommonForms() {
        assertEquals(new DiceExpression(2, 6, 3), DiceExpression.parse("2d6+3"));
        assertEquals(new DiceExpression(1, 8, 0), DiceExpression.parse("1d8"));
        assertEquals(new DiceExpression(1, 20, 0), DiceExpression.parse("d20"));
        assertEquals(new DiceExpression(1, 8, 0), DiceExpression.parse("1к8"));
    }

    @Test
    @DisplayName("Плоское число разбирается как чистый модификатор без кубов")
    void parse_flatNumberIsModifierOnly() {
        assertEquals(new DiceExpression(0, 0, 5), DiceExpression.parse("5"));
    }

    @Test
    @DisplayName("Пустое/нечитаемое выражение → null")
    void parse_blankOrJunkIsNull() {
        assertNull(DiceExpression.parse(""));
        assertNull(DiceExpression.parse(null));
        assertNull(DiceExpression.parse("мечом"));
    }

    @Test
    @DisplayName("Границы выражения: min — все единицы, max — все максимумы")
    void bounds_minAndMax() {
        DiceExpression d = DiceExpression.parse("2d6+3");
        assertEquals(5, d.min());   // 1 + 1 + 3
        assertEquals(15, d.max());  // 6 + 6 + 3
    }

    // ------------------------------ Бросок ------------------------------

    @RepeatedTest(50)
    @DisplayName("Бросок урона всегда в пределах [min, max] выражения")
    void rollDamage_staysWithinBounds() {
        int roll = diceRoller.rollDamage("2d6+3", false);
        assertTrue(roll >= 5 && roll <= 15, "ожидалось 5..15, получено " + roll);
    }

    @RepeatedTest(50)
    @DisplayName("Критический бросок удваивает кубы, но не плоский модификатор")
    void rollDamage_critDoublesDiceOnly() {
        int roll = diceRoller.rollDamage("2d6+3", true);
        // 4 куба d6 => 4..24, плюс +3 => 7..27
        assertTrue(roll >= 7 && roll <= 27, "ожидалось 7..27, получено " + roll);
    }

    @Test
    @DisplayName("Нечитаемое выражение урона даёт 0")
    void rollDamage_unparseableIsZero() {
        assertEquals(0, diceRoller.rollDamage("мечом", false));
        assertEquals(0, diceRoller.rollDamage(null, true));
    }
}
