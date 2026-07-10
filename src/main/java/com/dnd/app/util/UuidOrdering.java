package com.dnd.app.util;

import java.util.UUID;

/**
 * Класс UuidOrdering описывает утилиту, которая поддерживает повторяемые операции бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public final class UuidOrdering {

    private UuidOrdering() {
    }

    /**
     * Выполняет операции "compare unsigned" в рамках бизнес-логики приложения.
     * @param first входящее значение first, используемое бизнес-сценарием
     * @param second входящее значение second, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static int compareUnsigned(UUID first, UUID second) {
        int high = Long.compareUnsigned(first.getMostSignificantBits(), second.getMostSignificantBits());
        if (high != 0) {
            return high;
        }
        return Long.compareUnsigned(first.getLeastSignificantBits(), second.getLeastSignificantBits());
    }

    /**
     * Выполняет операции "normalized pair" в рамках бизнес-логики приложения.
     * @param first входящее значение first, используемое бизнес-сценарием
     * @param second входящее значение second, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static UUID[] normalizedPair(UUID first, UUID second) {
        return compareUnsigned(first, second) < 0
                ? new UUID[]{first, second}
                : new UUID[]{second, first};
    }
}
