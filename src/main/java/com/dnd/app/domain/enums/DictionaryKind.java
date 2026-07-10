package com.dnd.app.domain.enums;

import java.util.Arrays;

/**
 * Перечисление DictionaryKind описывает перечисление домена, которое фиксирует допустимые значения игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public enum DictionaryKind {
    CREATURE_TYPE("creature-types"),
    ALIGNMENT("alignments"),
    LANGUAGE("languages"),
    SENSE_TYPE("sense-types"),
    MOVEMENT_TYPE("movement-types"),
    HABITAT("habitats"),
    TREASURE_TAG("treasure-tags"),
    CONDITION("conditions"),
    GEAR_ITEM("gear-items"),
    SOURCE("sources"),
    SIZE("sizes"),
    ABILITY("abilities"),
    DAMAGE_TYPE("damage-types"),
    EQUIPMENT_SLOT("equipment-slots");

    private final String slug;

    DictionaryKind(String slug) {
        this.slug = slug;
    }

    /**
     * Возвращает результат операции "get slug" в рамках бизнес-логики домена.
     * @return результат выполнения бизнес-операции
     */
    public String getSlug() {
        return slug;
    }

    /**
     * Выполняет операции "from slug" в рамках бизнес-логики домена.
     * @param slug входящее значение slug, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static DictionaryKind fromSlug(String slug) {
        return Arrays.stream(values())
                .filter(k -> k.slug.equalsIgnoreCase(slug))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown dictionary: " + slug));
    }
}
