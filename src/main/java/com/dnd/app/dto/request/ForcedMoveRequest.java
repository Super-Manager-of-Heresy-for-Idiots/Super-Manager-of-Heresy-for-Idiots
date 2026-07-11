package com.dnd.app.dto.request;

import com.dnd.app.domain.enums.ForcedMoveType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Запрос на принудительное перемещение комбатанта (push/pull/slide, фаза 2.12). Позиции клеток
 * приходят с фронта (map-авторитетные, релеятся по требованию, как в 2.5); core проверяет дистанцию
 * против максимума эффекта и передаёт итоговую клетку в map. Перемещение не тратит движение цели и
 * не провоцирует атак.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForcedMoveRequest {

    /** Тип перемещения: PUSH/PULL/SLIDE. */
    @NotNull(message = "Forced move type is required")
    private ForcedMoveType type;

    /** Комбатант, которого перемещают. */
    @NotNull(message = "Target combatant ID is required")
    private UUID targetCombatantId;

    /** Целевая клетка по X. */
    @NotNull(message = "toCol is required")
    private Integer toCol;

    /** Целевая клетка по Y. */
    @NotNull(message = "toRow is required")
    private Integer toRow;

    /** Исходная клетка цели по X (для проверки дистанции); null — проверка пропускается. */
    private Integer fromCol;

    /** Исходная клетка цели по Y (для проверки дистанции); null — проверка пропускается. */
    private Integer fromRow;

    /** Максимальная дистанция эффекта в футах (для валидации); null — без ограничения. */
    private Integer maxDistanceFt;
}
