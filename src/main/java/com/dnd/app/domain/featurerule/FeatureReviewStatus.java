package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Optional;

/**
 * Перечисление FeatureReviewStatus описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public enum FeatureReviewStatus {

    /** Work in progress; not yet submitted for review. */
    DRAFT("draft"),
    /** Submitted / parser-generated; waiting for admin review. */
    NEEDS_REVIEW("needs_review"),
    /** Reviewed and approved; the only status eligible for runtime execution. */
    APPROVED("approved"),
    /** Explicitly turned off; kept for history but never executed. */
    DISABLED("disabled");

    private final String code;

    FeatureReviewStatus(String code) {
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
     * Проверяет условие операции "is runtime eligible" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    public boolean isRuntimeEligible() {
        return this == APPROVED;
    }

    /**
     * Выполняет операции "from code" в рамках бизнес-логики домена.
     * @param code входящее значение code, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static Optional<FeatureReviewStatus> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(s -> s.code.equalsIgnoreCase(code))
                .findFirst();
    }
}
