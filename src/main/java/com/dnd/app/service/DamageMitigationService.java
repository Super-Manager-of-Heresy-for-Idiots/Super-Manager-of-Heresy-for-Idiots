package com.dnd.app.service;

import com.dnd.app.domain.BattleCombatant;
import com.dnd.app.domain.Monster;
import com.dnd.app.domain.enums.CombatantType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Класс DamageMitigationService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Service
@RequiredArgsConstructor
public class DamageMitigationService {

    private final ModifierAggregator modifierAggregator;

    /**
     * Перечисление DamageModifier описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
     * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
     */
    public enum DamageModifier {
        NONE,
        RESISTED,
        IMMUNE,
        VULNERABLE
    }

    /**
     * Запись Mitigation описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
     * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
     * @param finalDamage входящее значение final damage, используемое бизнес-сценарием
     * @param modifier входящее значение modifier, используемое бизнес-сценарием
     */
    public record Mitigation(int finalDamage, DamageModifier modifier) {
    }

    /**
     * Выполняет операции "mitigate" в рамках бизнес-логики домена.
     * @param target входящее значение target, используемое бизнес-сценарием
     * @param damage входящее значение damage, используемое бизнес-сценарием
     * @param damageTypeId идентификатор damage type, используемый для выбора нужного бизнес-объекта
     * @return результат выполнения бизнес-операции
     */
    public Mitigation mitigate(BattleCombatant target, int damage, UUID damageTypeId) {
        if (target == null || damage <= 0 || damageTypeId == null) {
            return new Mitigation(Math.max(0, damage), DamageModifier.NONE);
        }
        DamageModifier modifier = resolveModifier(target, damageTypeId);
        int finalDamage = switch (modifier) {
            case IMMUNE -> 0;
            case RESISTED -> damage / 2; // floor(x / 2)
            case VULNERABLE -> damage * 2;
            case NONE -> damage;
        };
        return new Mitigation(finalDamage, modifier);
    }

    private DamageModifier resolveModifier(BattleCombatant target, UUID damageTypeId) {
        if (target.getType() == CombatantType.MONSTER && target.getMonster() != null) {
            Monster monster = target.getMonster();
            if (immuneTo(monster, damageTypeId)) {
                return DamageModifier.IMMUNE;
            }
            boolean resist = resistantTo(monster, damageTypeId);
            boolean vulnerable = vulnerableTo(monster, damageTypeId);
            if (resist && !vulnerable) {
                return DamageModifier.RESISTED;
            }
            if (vulnerable && !resist) {
                return DamageModifier.VULNERABLE;
            }
            return DamageModifier.NONE;
        }
        if (target.getType() == CombatantType.CHARACTER && target.getCharacter() != null) {
            double multiplier = modifierAggregator.damageMultiplier(target.getCharacter().getId(), damageTypeId);
            if (multiplier == 0.0) {
                return DamageModifier.IMMUNE;
            }
            if (multiplier < 1.0) {
                return DamageModifier.RESISTED;
            }
            if (multiplier > 1.0) {
                return DamageModifier.VULNERABLE;
            }
            return DamageModifier.NONE;
        }
        return DamageModifier.NONE;
    }

    private static boolean immuneTo(Monster monster, UUID damageTypeId) {
        return monster.getDamageImmunities().stream()
                .anyMatch(r -> r.getDamageType() != null && damageTypeId.equals(r.getDamageType().getId()));
    }

    private static boolean resistantTo(Monster monster, UUID damageTypeId) {
        return monster.getDamageResistances().stream()
                .anyMatch(r -> r.getDamageType() != null && damageTypeId.equals(r.getDamageType().getId()));
    }

    private static boolean vulnerableTo(Monster monster, UUID damageTypeId) {
        return monster.getDamageVulnerabilities().stream()
                .anyMatch(r -> r.getDamageType() != null && damageTypeId.equals(r.getDamageType().getId()));
    }
}
