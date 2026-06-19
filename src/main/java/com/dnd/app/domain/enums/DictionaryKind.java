package com.dnd.app.domain.enums;

import java.util.Arrays;

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

    public String getSlug() {
        return slug;
    }

    public static DictionaryKind fromSlug(String slug) {
        return Arrays.stream(values())
                .filter(k -> k.slug.equalsIgnoreCase(slug))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown dictionary: " + slug));
    }
}
