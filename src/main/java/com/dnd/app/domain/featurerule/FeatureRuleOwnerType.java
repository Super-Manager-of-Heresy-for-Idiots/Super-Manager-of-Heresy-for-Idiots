package com.dnd.app.domain.featurerule;

import java.util.Arrays;
import java.util.Set;
import java.util.Optional;

/**
 * Перечисление FeatureRuleOwnerType описывает доменную модель правил возможностей, которая хранит исполняемые игровые эффекты.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public enum FeatureRuleOwnerType {

    CLASS_FEATURE("CLASS_FEATURE"),
    BACKGROUND("BACKGROUND"),
    FEAT("FEAT"),
    SPELL("SPELL"),
    ITEM_TEMPLATE("ITEM_TEMPLATE"),
    ITEM_EQUIPMENT("ITEM_EQUIPMENT"),
    ITEM_MAGIC("ITEM_MAGIC");

    public static final Set<FeatureRuleOwnerType> ITEM_FAMILY = Set.of(
            ITEM_TEMPLATE,
            ITEM_EQUIPMENT,
            ITEM_MAGIC
    );

    private final String code;

    FeatureRuleOwnerType(String code) {
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
     * Проверяет, что владелец правила относится к семейству предметов.
     * @return true, если правило принадлежит определению предмета, а не классовой фиче/заклинанию
     */
    public boolean isItemOwner() {
        return ITEM_FAMILY.contains(this);
    }

    /**
     * Проверяет, что владелец правила относится к семейству предметов.
     * @param ownerType тип владельца правила
     * @return true, если переданный тип является предметным owner-типом
     */
    public static boolean isItemOwner(FeatureRuleOwnerType ownerType) {
        return ownerType != null && ownerType.isItemOwner();
    }

    /**
     * Выполняет операции "from code" в рамках бизнес-логики домена.
     * @param code входящее значение code, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static Optional<FeatureRuleOwnerType> fromCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(t -> t.code.equalsIgnoreCase(code))
                .findFirst();
    }
}
