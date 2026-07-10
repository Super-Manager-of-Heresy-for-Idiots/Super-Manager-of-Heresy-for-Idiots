package com.dnd.app.domain.enums;

/**
 * Перечисление CoverType описывает перечисление домена, которое фиксирует допустимые значения игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public enum CoverType {
    NONE(0),
    HALF(2),
    THREE_QUARTERS(5),
    TOTAL(0);

    private final int bonus;

    CoverType(int bonus) {
        this.bonus = bonus;
    }

    /**
     * Выполняет операции "bonus" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    public int bonus() {
        return bonus;
    }
}
