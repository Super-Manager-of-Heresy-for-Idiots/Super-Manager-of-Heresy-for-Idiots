package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/**
 * Перечисление FeatureRuleSource описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public enum FeatureRuleSource {

    /** Authored by an admin in the Rule Workbench. */
    MANUAL("manual"),
    /** Produced by the description parser/importer. */
    PARSER("parser"),
    /** Created by the content seed pipeline. */
    SEED("seed"),
    /** Created by a data migration/backfill. */
    MIGRATION("migration");

    private final String code;

    FeatureRuleSource(String code) {
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
    public static Optional<FeatureRuleSource> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.code.equalsIgnoreCase(code))
                .findFirst();
    }
}
