package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/**
 * Перечисление FeatureIssueSeverity описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public enum FeatureIssueSeverity {

    /** Informational note; does not block anything. */
    INFO("info"),
    /** Warning; should be reviewed but does not block approval. */
    WARN("warn"),
    /** Blocking problem; an unresolved error prevents approval of the rule. */
    ERROR("error");

    private final String code;

    FeatureIssueSeverity(String code) {
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
     * Выполняет операции "blocks approval" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    public boolean blocksApproval() {
        return this == ERROR;
    }

    /**
     * Выполняет операции "from code" в рамках бизнес-логики домена.
     * @param code входящее значение code, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static Optional<FeatureIssueSeverity> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.code.equalsIgnoreCase(code))
                .findFirst();
    }
}
