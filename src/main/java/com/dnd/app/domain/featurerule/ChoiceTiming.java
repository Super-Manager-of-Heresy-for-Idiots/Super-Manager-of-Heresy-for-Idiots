package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/**
 * Перечисление ChoiceTiming описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public enum ChoiceTiming {

    LEVEL_UP("level_up"),
    LONG_REST("long_rest"),
    SHORT_REST("short_rest"),
    ALWAYS_AVAILABLE("always_available"),
    MANUAL_ADMIN("manual_admin");

    private final String code;

    ChoiceTiming(String code) {
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
     * Выполняет операции "from code" в рамках бизнес-логики домена.
     * @param code входящее значение code, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static Optional<ChoiceTiming> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values()).filter(v -> v.code.equalsIgnoreCase(code)).findFirst();
    }
}
