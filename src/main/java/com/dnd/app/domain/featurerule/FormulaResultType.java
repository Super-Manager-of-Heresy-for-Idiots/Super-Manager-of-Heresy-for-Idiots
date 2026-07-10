package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/**
 * Перечисление FormulaResultType описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public enum FormulaResultType {

    INTEGER("integer"),
    DECIMAL("decimal"),
    BOOLEAN("boolean"),
    DURATION("duration"),
    DICE("dice"),
    MODIFIER("modifier");

    private final String code;

    FormulaResultType(String code) {
        this.code = code;
    }

    /**
     * Возвращает результат операции "get code" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    public String getCode() {
        return code;
    }

    /**
     * Проверяет условие операции "is numeric" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    public boolean isNumeric() {
        return this == INTEGER || this == DECIMAL || this == DURATION || this == MODIFIER;
    }

    /**
     * Выполняет операции "from code" в рамках бизнес-логики домена.
     * @param code входящее значение code, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static Optional<FormulaResultType> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(v -> v.code.equalsIgnoreCase(code)).findFirst();
    }
}
