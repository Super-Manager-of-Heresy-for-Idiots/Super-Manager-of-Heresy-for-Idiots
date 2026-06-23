package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CharacterAttackResponse {
    private String name;
    private String attackBonus;
    private String damage;
    private String damageType;

    /** MELEE | RANGED | THROWN — lets the combat UI group strikes, shots and thrown attacks. Null = unspecified. */
    private String category;

    /** WEAPON | CLASS | MANUAL — where this attack comes from (equipped weapon, class feature, or hand-authored). */
    private String source;

    /** Optional reach/range hint, e.g. "5 фт." for melee or "20/60 фт." for thrown/ranged. */
    private String range;
}
