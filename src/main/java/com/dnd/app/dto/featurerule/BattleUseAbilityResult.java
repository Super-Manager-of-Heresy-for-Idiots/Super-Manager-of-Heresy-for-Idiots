package com.dnd.app.dto.featurerule;

import com.dnd.app.dto.response.BattleResponse;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Результат боевого использования умения через feature-rules runtime.
 *
 * @param featureId идентификатор использованного умения
 * @param featureName отображаемое имя умения
 * @param actionType код потраченного типа действия
 * @param resourceKey ключ ресурса, если умение тратило ресурс
 * @param resourceSpent сколько ресурса потрачено
 * @param resourceRemaining сколько ресурса осталось
 * @param logId идентификатор runtime-лога feature_use
 * @param outcome машинный итог выполнения
 * @param targetCombatantId основная цель умения, если была выбрана
 * @param targetName имя основной цели, если была выбрана
 * @param plan структурированный план исполнения правила
 * @param appliedDamage итоговый нанесенный урон после сейвов и сопротивлений
 * @param appliedDamageModifier примененный модификатор урона
 * @param battle актуальное состояние боя после применения
 * @param message человекочитаемое сообщение результата
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleUseAbilityResult {
    private UUID featureId;
    private String featureName;
    private String actionType;
    private String resourceKey;
    private Integer resourceSpent;
    private Integer resourceRemaining;
    private UUID logId;
    private String outcome;
    private UUID targetCombatantId;
    private String targetName;
    private FeatureExecutionPlan plan;
    private Integer appliedDamage;
    private String appliedDamageModifier;
    private BattleResponse battle;
    private String message;
}
