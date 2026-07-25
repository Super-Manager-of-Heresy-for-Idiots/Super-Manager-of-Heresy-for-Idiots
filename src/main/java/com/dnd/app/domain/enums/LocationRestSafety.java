package com.dnd.app.domain.enums;

/**
 * Перечисление LocationRestSafety описывает метку безопасности привала на локации кампании.
 * Значение не применяет механику: оно управляет подсказками мастеру на экране лагеря
 * (предложить бросок случайной встречи или засады).
 */
public enum LocationRestSafety {
    SAFE,
    RISKY,
    DANGEROUS
}
