package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A class feature the character has unlocked at their current level, surfaced in the combat
 * panel so leveled-up characters can see (and use) their class-specific actions. Features whose
 * description carries a damage expression are also exposed as resolvable attacks
 * ({@link #usableAsAttack} = true) via the normal attack endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassAbilityResponse {
    private String name;
    private Integer level;
    private String className;
    private String description;
    private boolean usableAsAttack;
    /** Damage expression when usable as an attack (e.g. "1d6"); null otherwise. */
    private String damage;
}
