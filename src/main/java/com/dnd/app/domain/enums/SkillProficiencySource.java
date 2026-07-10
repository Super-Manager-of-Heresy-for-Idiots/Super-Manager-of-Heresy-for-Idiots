package com.dnd.app.domain.enums;

/**
 * Перечисление SkillProficiencySource описывает перечисление домена, которое фиксирует допустимые значения игровой бизнес-логики.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
public enum SkillProficiencySource {
    CLASS,
    BACKGROUND,
    RACE,
    MANUAL,
    /** Granted by a class-feature rule (feature-rules runtime, Stage 4). */
    FEATURE
}
