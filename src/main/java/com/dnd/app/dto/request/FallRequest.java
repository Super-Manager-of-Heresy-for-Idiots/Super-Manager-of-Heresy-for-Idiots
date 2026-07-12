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
 * Падение комбатанта с высоты (фаза 3.4). Урон падения — 1к6 за каждые 10 футов высоты (кап 20к6),
 * приземление обычно валит цель ничком (prone). Реюзает примитив {@code applyDamageOrHeal} и
 * {@code ConditionService.applyByCode("prone")}; пишет лог {@code FALL}. Если комбатант летел —
 * падение сбрасывает флаг полёта. Триггер — снятие полёта на высоте или ручная GM-кнопка.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FallRequest {

    /** Падающий комбатант. */
    @NotNull(message = "Combatant ID is required")
    private UUID combatantId;

    /** Высота падения в футах (определяет число к6: floor(высота/10), кап 20). */
    @NotNull(message = "Height in feet is required")
    @Min(value = 0, message = "heightFt must be >= 0")
    private Integer heightFt;

    /** Готовый итоговый урон (GM/фронт уже бросил кубы); null — сервер бросит сам к6 по высоте. */
    private Integer manualTotal;

    /** Валить ли цель ничком (prone) при приземлении; null трактуется как true. */
    private Boolean applyProne;
}
