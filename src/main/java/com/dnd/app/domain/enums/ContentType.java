package com.dnd.app.domain.enums;

/**
 * Перечисление ContentType описывает перечисление домена, которое фиксирует допустимые значения игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public enum ContentType {
    ITEM_TYPE,
    CHARACTER_CLASS,
    SKILL,
    FEAT,
    SUBCLASS,
    RACE,
    SPECIES,
    STAT_TYPE,
    BUFF_DEBUFF,
    ENCHANTMENT_TYPE,
    CURRENCY,
    CUSTOM_RESOURCE,
    ITEM_TEMPLATE,
    BACKGROUND,
    SPELL,
    PROFICIENCY_SKILL,
    MONSTER
}
