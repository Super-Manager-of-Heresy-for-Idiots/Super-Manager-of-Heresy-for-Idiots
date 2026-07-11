package com.dnd.app.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Запрос на телепортацию комбатанта, при необходимости с прихватом ближайших союзников (фаза 2.12).
 * Позиции клеток приходят с фронта (map-авторитетные); core проверяет, что цель телепорта в пределах
 * дальности заклинания, а каждый прихватываемый союзник — рядом с телепортируемым (в радиусе
 * {@code allyPickupFt}) и его точка назначения в пределах дальности. Итоговые клетки передаются в map.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeleportRequest {

    /** Комбатант, который телепортируется (инициатор). */
    @NotNull(message = "Combatant ID is required")
    private UUID combatantId;

    /** Точка назначения телепортируемого по X. */
    @NotNull(message = "toCol is required")
    private Integer toCol;

    /** Точка назначения телепортируемого по Y. */
    @NotNull(message = "toRow is required")
    private Integer toRow;

    /** Исходная клетка телепортируемого по X (для проверки дальности и радиуса прихвата). */
    private Integer fromCol;

    /** Исходная клетка телепортируемого по Y (для проверки дальности и радиуса прихвата). */
    private Integer fromRow;

    /** Дальность телепортации в футах (для валидации); null — без ограничения. */
    private Integer rangeFt;

    /** Максимальный радиус в футах, в котором союзник может быть, чтобы его прихватить; null — без проверки. */
    private Integer allyPickupFt;

    /** Прихватываемые союзники (может быть пустым/null). */
    private List<Ally> allies;

    /**
     * Прихватываемый союзник и его точка назначения.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Ally {
        /** Комбатант-союзник. */
        @NotNull(message = "Ally combatant ID is required")
        private UUID combatantId;

        /** Точка назначения союзника по X. */
        @NotNull(message = "Ally toCol is required")
        private Integer toCol;

        /** Точка назначения союзника по Y. */
        @NotNull(message = "Ally toRow is required")
        private Integer toRow;

        /** Исходная клетка союзника по X (для проверки радиуса прихвата). */
        private Integer fromCol;

        /** Исходная клетка союзника по Y (для проверки радиуса прихвата). */
        private Integer fromRow;
    }
}
