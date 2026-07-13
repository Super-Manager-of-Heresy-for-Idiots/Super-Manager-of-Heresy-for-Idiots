package com.dnd.app.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Запрос на использование активного умения в бою через feature-rules runtime.
 *
 * @param featureId идентификатор class feature, которым владеет активный персонаж
 * @param combatantId актор реакции вне текущего хода; если не задан, используется активный комбатант
 * @param targetCombatantId одиночная цель умения в бою
 * @param targetCombatantIds набор целей для массового применения результата плана
 * @param originX координата X шаблона области на карте
 * @param originY координата Y шаблона области на карте
 * @param rotationDeg поворот шаблона области в градусах
 * @param damageRollMode режим броска урона: AUTO или MANUAL
 * @param manualDamage вручную введенный итог урона до сейва и сопротивлений
 * @param clientCommandId идемпотентный ключ клиентской команды
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleUseAbilityRequest {

    @NotNull(message = "featureId is required")
    private UUID featureId;

    private UUID combatantId;

    private UUID itemInstanceId;

    private UUID targetCombatantId;

    private List<UUID> targetCombatantIds;

    private Integer originX;
    private Integer originY;
    private Double rotationDeg;

    private String damageRollMode;

    @Min(value = 0, message = "manualDamage must be >= 0")
    private Integer manualDamage;

    private UUID clientCommandId;
}
