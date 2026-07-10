package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/**
 * Перечисление FormulaRoundingMode описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public enum FormulaRoundingMode {

    FLOOR("floor"),
    CEIL("ceil"),
    NEAREST("nearest"),
    NONE("none");

    private final String code;

    FormulaRoundingMode(String code) {
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
     * Выполняет операции "apply" в рамках бизнес-логики домена.
     * @param value входящее значение value, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public double apply(double value) {
        return switch (this) {
            case FLOOR -> Math.floor(value);
            case CEIL -> Math.ceil(value);
            case NEAREST -> Math.rint(value);
            case NONE -> value;
        };
    }

    /**
     * Выполняет операции "from code" в рамках бизнес-логики домена.
     * @param code входящее значение code, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static Optional<FormulaRoundingMode> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(v -> v.code.equalsIgnoreCase(code)).findFirst();
    }
}
