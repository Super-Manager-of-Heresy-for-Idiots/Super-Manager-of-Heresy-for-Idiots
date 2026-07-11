package com.dnd.app.integration.map;

import java.util.List;
import java.util.UUID;

/**
 * Контракт MapTokenMover связывает core с map-сервисом для принудительного перемещения токенов
 * (фаза 2.12): push/pull/slide и телепорт (в т.ч. телепорт с прихватом союзников). Core валидирует
 * дистанцию/дальность и передаёт итоговые клетки; map исполняет перемещение и рассылает событие.
 */
public interface MapTokenMover {

    /**
     * Принудительно перемещает токены комбатантов на карте боя.
     *
     * @param battleId идентификатор боя, на сессиях которого исполняется перемещение
     * @param spec     тип перемещения и список «комбатант → целевая клетка»
     */
    void forcedMove(UUID battleId, ForcedMoveSpec spec);

    /**
     * Спецификация принудительного перемещения.
     *
     * @param movementType тип перемещения (PUSH/PULL/SLIDE/TELEPORT) для пометки в событии/логе
     * @param moves        список перемещений токенов
     */
    record ForcedMoveSpec(String movementType, List<TokenMove> moves) {
    }

    /**
     * Одно перемещение токена.
     *
     * @param combatantId внешний id комбатанта (map резолвит его в токен)
     * @param toX         целевая клетка по X
     * @param toY         целевая клетка по Y
     */
    record TokenMove(UUID combatantId, int toX, int toY) {
    }
}
