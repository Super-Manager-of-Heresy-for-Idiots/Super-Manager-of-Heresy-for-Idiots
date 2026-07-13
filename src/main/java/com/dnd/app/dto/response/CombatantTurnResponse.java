package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.dnd.app.dto.featurerule.AvailableFeatureAction;

import java.util.List;

/**
 * Класс CombatantTurnResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CombatantTurnResponse {

    private BattleCombatantResponse combatant;

    // Populated when the active combatant is a CHARACTER
    private CharacterResponse character;
    private List<ResourceResponse> resources;
    private List<CharacterActiveEffectResponse> activeEffects;
    /** Class features unlocked at the character's current level (progression-based actions). */
    private List<ClassAbilityResponse> classAbilities;
    /** Server-calculated class feature actions available in this battle turn. */
    private List<AvailableFeatureAction> featureActions;
    /** Spell slots (per level: max / expended / available). Null when the class has no slots. */
    private SpellSlotsResponse spellSlots;

    // Populated when the active combatant is a MONSTER
    private MonsterResponse monster;

    /**
     * Read-only targeting metadata for every actionable option of the current combatant, so the
     * frontend tactical map can draw range/AoE previews. Additive — existing fields are untouched.
     */
    private List<TacticalActionResponse> tacticalActions;
}
