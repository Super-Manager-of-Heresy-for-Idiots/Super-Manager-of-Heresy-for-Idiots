package com.dnd.app.service.combat;

import com.dnd.app.domain.BattleCombatant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CombatCalculator: инициатива, опасность/опыт группы и порядок трекера")
class CombatCalculatorTest {

    // ------------------------------ Модификатор ------------------------------

    @Test
    @DisplayName("Модификатор характеристики = floor((значение - 10) / 2)")
    void abilityModifier_followsDndRule() {
        assertEquals(0, CombatCalculator.abilityModifier(10));
        assertEquals(0, CombatCalculator.abilityModifier(11));
        assertEquals(3, CombatCalculator.abilityModifier(16));
        assertEquals(-1, CombatCalculator.abilityModifier(8));
        assertEquals(-5, CombatCalculator.abilityModifier(1));
    }

    // ------------------------------ Инициатива ------------------------------

    @Test
    @DisplayName("Инициатива персонажа = d20 + модификатор Ловкости + бонус баффов")
    void characterInitiative_addsDexModifierAndBuff() {
        // d20=15, DEX 16 (+3), бафф +2 => 20
        assertEquals(20, CombatCalculator.characterInitiative(15, 16, 2));
        // дебафф учитывается как отрицательный бонус
        assertEquals(11, CombatCalculator.characterInitiative(10, 14, -1));
        // без баффов и с DEX 10 — просто d20
        assertEquals(7, CombatCalculator.characterInitiative(7, 10, 0));
    }

    @Test
    @DisplayName("Инициатива монстра: авторский бонус имеет приоритет над модификатором DEX")
    void monsterInitiative_prefersAuthoredBonus() {
        // бонус задан явно => используется он, а не модификатор от DEX
        assertEquals(17, CombatCalculator.monsterInitiative(12, 5, 8));
        // бонус не задан => берётся модификатор от DEX 18 (+4)
        assertEquals(16, CombatCalculator.monsterInitiative(12, null, 18));
    }

    // --------------------------- Опасность и опыт ---------------------------

    @Test
    @DisplayName("Средняя опасность = среднее CR, округлённое до 2 знаков")
    void averageDanger_meanRoundedToTwoDecimals() {
        BigDecimal result = CombatCalculator.averageDanger(List.of(
                new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("2")));
        assertEquals(new BigDecimal("1.67"), result);
    }

    @Test
    @DisplayName("Средняя опасность пустой группы = 0.00")
    void averageDanger_emptyGroupIsZero() {
        assertEquals(new BigDecimal("0.00"), CombatCalculator.averageDanger(List.of()));
        assertEquals(new BigDecimal("0.00"), CombatCalculator.averageDanger(null));
    }

    @Test
    @DisplayName("Суммарный опыт = сумма базового XP, null считается как 0")
    void totalXp_sumsBasesIgnoringNulls() {
        List<Integer> bases = new ArrayList<>();
        bases.add(100);
        bases.add(null);
        bases.add(50);
        assertEquals(150, CombatCalculator.totalXp(bases, null));
    }

    @Test
    @DisplayName("Переопределённый GM опыт имеет приоритет над суммой")
    void totalXp_overrideWins() {
        assertEquals(999, CombatCalculator.totalXp(List.of(100, 50), 999));
        assertEquals(0, CombatCalculator.totalXp(List.of(100, 50), 0));
    }

    // ----------------------------- Порядок трекера -----------------------------

    @Test
    @DisplayName("Трекер сортируется по инициативе (убыв.), затем по DEX, и проставляет turnOrder")
    void orderTracker_sortsByInitiativeThenDex() {
        BattleCombatant a = combatant(10, 12, Instant.parse("2026-01-01T00:00:00Z"));
        BattleCombatant b = combatant(18, 10, Instant.parse("2026-01-01T00:00:01Z"));
        BattleCombatant c = combatant(10, 16, Instant.parse("2026-01-01T00:00:02Z"));

        List<BattleCombatant> ordered = CombatCalculator.orderTracker(new ArrayList<>(List.of(a, b, c)));

        // b (18) первый; среди двух по 10 побеждает больший DEX (c=16 раньше a=12)
        assertSame(b, ordered.get(0));
        assertSame(c, ordered.get(1));
        assertSame(a, ordered.get(2));
        assertEquals(0, b.getTurnOrder());
        assertEquals(1, c.getTurnOrder());
        assertEquals(2, a.getTurnOrder());
    }

    @Test
    @DisplayName("Равные инициатива и DEX упорядочиваются стабильно по времени создания")
    void orderTracker_stableTieBreakByCreatedAt() {
        BattleCombatant earlier = combatant(10, 10, Instant.parse("2026-01-01T00:00:00Z"));
        BattleCombatant later = combatant(10, 10, Instant.parse("2026-01-01T00:00:05Z"));

        List<BattleCombatant> ordered = CombatCalculator.orderTracker(new ArrayList<>(List.of(later, earlier)));

        assertSame(earlier, ordered.get(0));
        assertSame(later, ordered.get(1));
    }

    @Test
    @DisplayName("Очередь хода остаётся на том же участнике после пересортировки")
    void resolveCurrentIndex_keepsTurnOnSameCombatant() {
        BattleCombatant active = combatant(12, 10, Instant.parse("2026-01-01T00:00:00Z"));
        BattleCombatant fast = combatant(20, 10, Instant.parse("2026-01-01T00:00:01Z"));
        // активный был под индексом 0; вступает более быстрый => активный смещается на 1
        List<BattleCombatant> reordered = CombatCalculator.orderTracker(
                new ArrayList<>(List.of(active, fast)));

        int index = CombatCalculator.resolveCurrentIndex(reordered, active.getId(), 0);
        assertEquals(1, index);
        assertSame(active, reordered.get(index));
    }

    @Test
    @DisplayName("Если активный участник исчез, индекс зажимается в границах")
    void resolveCurrentIndex_clampsWhenActiveMissing() {
        List<BattleCombatant> ordered = new ArrayList<>(List.of(
                combatant(10, 10, Instant.now()),
                combatant(8, 10, Instant.now())));

        assertEquals(1, CombatCalculator.resolveCurrentIndex(ordered, UUID.randomUUID(), 5));
        assertEquals(0, CombatCalculator.resolveCurrentIndex(ordered, null, -3));
    }

    private static BattleCombatant combatant(int initiative, int dexTiebreak, Instant createdAt) {
        return BattleCombatant.builder()
                .id(UUID.randomUUID())
                .initiative(initiative)
                .dexTiebreak(dexTiebreak)
                .createdAt(createdAt)
                .build();
    }
}
