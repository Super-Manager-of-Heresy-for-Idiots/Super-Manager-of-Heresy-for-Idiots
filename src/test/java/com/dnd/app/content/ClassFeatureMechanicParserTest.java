package com.dnd.app.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClassFeatureMechanicParserTest {

    @Test
    void parsesBonusActionHealingDice() {
        ClassFeatureMechanicParser.Result result = ClassFeatureMechanicParser.parse(
                "Второе дыхание",
                "Бонусным действием восстановите 1к10 + 5 хитов.");

        assertEquals("BONUS_ACTION", result.activationType());
        assertEquals("1d10 + 5", result.healingDice());
        assertNull(result.healingFlat());
        assertFalse(result.warning());
    }

    @Test
    void parsesSaveAbilityAndDamageType() {
        ClassFeatureMechanicParser.Result result = ClassFeatureMechanicParser.parse(
                "Огненный взрыв",
                "Цель совершает спасбросок Ловкости и получает 2к6 урона огнём.");

        assertEquals("DEXTERITY", result.saveAbility());
        assertEquals("2d6", result.damageDice());
        assertEquals("FIRE", result.damageType());
        assertFalse(result.warning());
    }

    @Test
    void flagsUnresolvedSavingThrow() {
        ClassFeatureMechanicParser.Result result = ClassFeatureMechanicParser.parse(
                "Странная защита",
                "Цель совершает спасбросок удачи.");

        assertTrue(result.warning());
        assertEquals(ClassFeatureMechanicParser.WARN_SAVE_UNRESOLVED, result.warningReason());
    }

    @Test
    void flagsMultipleDiceExpressions() {
        ClassFeatureMechanicParser.Result result = ClassFeatureMechanicParser.parse(
                "Сложный удар",
                "Попадание наносит 1к6 рубящего урона и 2к8 урона излучением.");

        assertEquals("1d6", result.damageDice());
        assertTrue(result.warning());
        assertEquals(ClassFeatureMechanicParser.WARN_MULTIPLE_DICE, result.warningReason());
    }
}
