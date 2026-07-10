package com.dnd.app.dto.combat;

import java.util.UUID;

/**
 * Запись ModifierTarget описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 * @param kind входящее значение kind, используемое бизнес-сценарием
 * @param statTypeId идентификатор stat type, используемый для выбора нужного бизнес-объекта
 * @param statSlug входящее значение stat slug, используемое бизнес-сценарием
 * @param damageTypeId идентификатор damage type, используемый для выбора нужного бизнес-объекта
 */
public record ModifierTarget(Kind kind, UUID statTypeId, String statSlug, UUID damageTypeId) {

    /**
     * Перечисление Kind описывает DTO, который переносит данные между API и бизнес-логикой.
     * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
     */
    public enum Kind { STAT_CHECK, SAVE, AC, ATTACK_ROLL, DAMAGE_DEALT, DAMAGE_TAKEN, INITIATIVE }

    /**
     * Выполняет операции "stat check" в рамках бизнес-логики передачи данных.
     * @param statTypeId идентификатор stat type, используемый для выбора нужного бизнес-объекта
     * @param statSlug входящее значение stat slug, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static ModifierTarget statCheck(UUID statTypeId, String statSlug) {
        return new ModifierTarget(Kind.STAT_CHECK, statTypeId, statSlug, null);
    }

    /**
     * Выполняет операции "save" в рамках бизнес-логики передачи данных.
     * @param statTypeId идентификатор stat type, используемый для выбора нужного бизнес-объекта
     * @param statSlug входящее значение stat slug, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static ModifierTarget save(UUID statTypeId, String statSlug) {
        return new ModifierTarget(Kind.SAVE, statTypeId, statSlug, null);
    }

    /**
     * Выполняет операции "ac" в рамках бизнес-логики передачи данных.
     * @return результат выполнения бизнес-операции
     */
    public static ModifierTarget ac() {
        return new ModifierTarget(Kind.AC, null, null, null);
    }

    /**
     * Выполняет операции "attack roll" в рамках бизнес-логики передачи данных.
     * @return результат выполнения бизнес-операции
     */
    public static ModifierTarget attackRoll() {
        return new ModifierTarget(Kind.ATTACK_ROLL, null, null, null);
    }

    /**
     * Выполняет операции "damage dealt" в рамках бизнес-логики передачи данных.
     * @return результат выполнения бизнес-операции
     */
    public static ModifierTarget damageDealt() {
        return new ModifierTarget(Kind.DAMAGE_DEALT, null, null, null);
    }

    /**
     * Выполняет операции "damage taken" в рамках бизнес-логики передачи данных.
     * @param damageTypeId идентификатор damage type, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    public static ModifierTarget damageTaken(UUID damageTypeId) {
        return new ModifierTarget(Kind.DAMAGE_TAKEN, null, null, damageTypeId);
    }

    /**
     * Выполняет операции "initiative" в рамках бизнес-логики передачи данных.
     * @param dexSlug входящее значение dex slug, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static ModifierTarget initiative(String dexSlug) {
        return new ModifierTarget(Kind.INITIATIVE, null, dexSlug, null);
    }
}
