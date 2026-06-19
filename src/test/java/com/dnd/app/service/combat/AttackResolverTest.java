package com.dnd.app.service.combat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
