package com.dnd.app.dto.content.grant;

/**
 * Перечисление GrantType описывает DTO, который переносит данные между API и бизнес-логикой.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public enum GrantType {
    FEATURE,
    SUBCLASS,
    FEAT,
    SPELL,
    SKILL_PROFICIENCY,
    ABILITY_SCORE,
    NUMERIC_MODIFIER,
    CUSTOM_TEXT;

    /**
     * Выполняет операции "from text or custom" в рамках бизнес-логики передачи данных.
     * @param raw входящее значение raw, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
    public static GrantType fromTextOrCustom(String raw) {
        if (raw == null) {
            return CUSTOM_TEXT;
        }
        for (GrantType type : values()) {
            if (type.name().equalsIgnoreCase(raw.trim())) {
                return type;
            }
        }
        return CUSTOM_TEXT;
    }
}
