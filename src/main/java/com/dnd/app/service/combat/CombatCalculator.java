package com.dnd.app.service.combat;

import com.dnd.app.domain.BattleCombatant;
import com.dnd.app.util.AbilityScores;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Класс CombatCalculator описывает сервис боевой логики, который рассчитывает и применяет правила боя.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public final class CombatCalculator {

    private CombatCalculator() {
    }

    /**
     * Выполняет операции "ability modifier" в рамках бизнес-логики боя.
     * @param score входящее значение score, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static int abilityModifier(int score) {
        return AbilityScores.modifier(score);
    }

    /**
     * Выполняет операции "character initiative" в рамках бизнес-логики боя.
     * @param d20 входящее значение d20, используемое бизнес-сценарием
     * @param dexScore входящее значение dex score, используемое бизнес-сценарием
     * @param dexBuffBonus входящее значение dex buff bonus, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static int characterInitiative(int d20, int dexScore, int dexBuffBonus) {
        return d20 + abilityModifier(dexScore) + dexBuffBonus;
    }

    /**
     * Выполняет операции "monster initiative" в рамках бизнес-логики боя.
     * @param d20 входящее значение d20, используемое бизнес-сценарием
     * @param initiativeBonus входящее значение initiative bonus, используемое бизнес-сценарием
     * @param dexScore входящее значение dex score, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static int monsterInitiative(int d20, Integer initiativeBonus, int dexScore) {
        int modifier = (initiativeBonus != null) ? initiativeBonus : abilityModifier(dexScore);
        return d20 + modifier;
    }

    /**
     * Выполняет операции "average danger" в рамках бизнес-логики боя.
     * @param crValues входящее значение cr values, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static BigDecimal averageDanger(List<BigDecimal> crValues) {
        if (crValues == null || crValues.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal sum = crValues.stream()
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(crValues.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * Преобразует данные операции "total xp" в рамках бизнес-логики боя.
     * @param xpBases входящее значение xp bases, используемое бизнес-сценарием
     * @param override входящее значение override, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static int totalXp(List<Integer> xpBases, Integer override) {
        if (override != null) {
            return override;
        }
        if (xpBases == null) {
            return 0;
        }
        return xpBases.stream().filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
    }

    /**
     * Выполняет операции "order tracker" в рамках бизнес-логики боя.
     * @param combatants входящее значение combatants, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static List<BattleCombatant> orderTracker(List<BattleCombatant> combatants) {
        combatants.sort(Comparator
                .comparing(BattleCombatant::getInitiative, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(BattleCombatant::getDexTiebreak, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(c -> c.getCreatedAt() == null ? Instant.EPOCH : c.getCreatedAt())
                .thenComparing(c -> c.getId() == null ? "" : c.getId().toString()));
        for (int i = 0; i < combatants.size(); i++) {
            combatants.get(i).setTurnOrder(i);
        }
        return combatants;
    }

    /**
     * Выполняет операции "resolve current index" в рамках бизнес-логики боя.
     * @param ordered входящее значение ordered, используемое бизнес-сценарием
     * @param activeCombatantId идентификатор active combatant, используемый для выбора нужного бизнес-объекта
     * @param previousIndex входящее значение previous index, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static int resolveCurrentIndex(List<BattleCombatant> ordered, UUID activeCombatantId, int previousIndex) {
        if (activeCombatantId != null) {
            for (int i = 0; i < ordered.size(); i++) {
                if (activeCombatantId.equals(ordered.get(i).getId())) {
                    return i;
                }
            }
        }
        if (ordered.isEmpty()) {
            return 0;
        }
        return Math.min(Math.max(previousIndex, 0), ordered.size() - 1);
    }
}
