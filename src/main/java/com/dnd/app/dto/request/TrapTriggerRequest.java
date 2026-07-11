package com.dnd.app.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Срабатывание ловушки по цели (фаза 3.2). Геометрия/параметры ловушки живут на карте (map-элемент);
 * фронт релеит параметры сюда, core резолвит спасбросок и урон переиспользуя примитивы сейва/митигации
 * (как в bulkAction) и пишет лог {@code TRAP}. Триггер — ручной GM или по входу (детектится на FE).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrapTriggerRequest {

    /** Комбатант, попавший в ловушку. */
    @NotNull(message = "Target combatant ID is required")
    private UUID targetCombatantId;

    /** Итоговый урон ловушки (фронт/GM уже бросил кубы); 0 — только эффект без урона. */
    private Integer amount;

    /** DC спасброска; null — сейва нет (урон полный). */
    private Integer saveDc;

    /** Способность спасброска (bestiary-код, напр. DEXTERITY); null — сейва нет. */
    private String saveAbility;

    /** Половинный урон при успешном сейве (иначе успех = 0 урона). */
    private Boolean halfOnSave;

    /** Тип урона для сопротивлений/иммунитета; null — без типизации. */
    private UUID damageTypeId;

    /** Ручной d20 спасброска цели; omit — сервер бросит сам (AUTO). */
    @Min(value = 1, message = "saveD20 must be between 1 and 20")
    @Max(value = 20, message = "saveD20 must be between 1 and 20")
    private Integer saveD20;

    /** Подпись ловушки для лога (например, «Яма с кольями»). */
    private String label;
}
