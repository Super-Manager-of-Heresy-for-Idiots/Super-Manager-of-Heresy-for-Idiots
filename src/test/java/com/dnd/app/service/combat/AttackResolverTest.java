package com.dnd.app.service.combat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@DisplayName("AttackResolver: попадание/промах/крит и разбор бонуса атаки")
class AttackResolverTest {

    @Test
    @DisplayName("Натуральная 20 — всегда критическое попадание, даже если не пробивает AC")
    void naturalTwenty_isAlwaysCrit() {
        assertEquals(AttackResolver.Outcome.CRIT, AttackResolver.resolve(20, -5, 99));
    }

    @Test
    @DisplayName("Натуральная 1 — всегда промах, даже с огромным бонусом")
    void naturalOne_isAlwaysMiss() {
        assertEquals(AttackResolver.Outcome.MISS, AttackResolver.resolve(1, 50, 5));
    }

    @Test
    @DisplayName("Попадание, когда d20 + бонус достигает AC")
    void hit_whenTotalReachesAc() {
        // 14 + 3 = 17 >= 17 => попадание
        assertEquals(AttackResolver.Outcome.HIT, AttackResolver.resolve(14, 3, 17));
        // 14 + 2 = 16 < 17 => промах
        assertEquals(AttackResolver.Outcome.MISS, AttackResolver.resolve(14, 2, 17));
    }

    @Test
    @DisplayName("Разбор бонуса атаки: '+5', '5', '-1', мусор и пусто")
    void parseAttackBonus_handlesSignsAndJunk() {
        assertEquals(5, AttackResolver.parseAttackBonus("+5"));
        assertEquals(5, AttackResolver.parseAttackBonus("5"));
        assertEquals(-1, AttackResolver.parseAttackBonus("-1"));
        assertEquals(0, AttackResolver.parseAttackBonus(""));
        assertEquals(0, AttackResolver.parseAttackBonus(null));
        assertEquals(0, AttackResolver.parseAttackBonus("abc"));
    }

    @Test
    @DisplayName("Урон извлекается из описания атаки монстра: кости в скобках")
    void extractDamage_diceInParens() {
        String desc = "Коготь . Рукопашная атака оружием : +5 , досягаемость 5 фт. " +
                "Попадание : 5 ( 1к4 + 3 ) колющего урона.";
        assertEquals("1к4 + 3", AttackResolver.extractDamageExpression(desc));
    }

    @Test
    @DisplayName("Урон извлекается из описания: плоское число без костей")
    void extractDamage_flatNumber() {
        String desc = "Укус . Рукопашная атака оружием : +2 , досягаемость 5 фт. " +
                "Попадание : 1 дробящего урона.";
        assertEquals("1", AttackResolver.extractDamageExpression(desc));
    }

    @Test
    @DisplayName("Якорь на клаузу попадания: не хватает бонус к попаданию или досягаемость")
    void extractDamage_anchorsOnHitClause() {
        String desc = "Дальнобойная атака : +5 , дистанция 25/50 фт. " +
                "Попадание : 10 ( 2к6 + 3 ) колющего урона";
        assertEquals("2к6 + 3", AttackResolver.extractDamageExpression(desc));
    }

    @Test
    @DisplayName("Нет урона в описании → null")
    void extractDamage_noneIsNull() {
        assertNull(AttackResolver.extractDamageExpression("Особая способность без урона."));
        assertNull(AttackResolver.extractDamageExpression(null));
        assertNull(AttackResolver.extractDamageExpression(""));
    }
}
