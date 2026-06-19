package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Everything the battle UI needs for whoever's turn it is. For a character it bundles the
 * full sheet (stats, known spells, attacks, features), the character's resources (rage,
 * spell slots, …) and active effects. For a monster the {@code monster} block is populated
 * instead (visible to the GM).
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

    // Populated when the active combatant is a MONSTER
    private MonsterResponse monster;
}
