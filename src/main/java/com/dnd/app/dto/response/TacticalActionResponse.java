package com.dnd.app.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Read-only targeting metadata for one actionable option (weapon strike, spell, class ability or
 * monster feature) of the combatant whose turn it is. It exists so the frontend tactical map can
 * draw targeting previews (range rings, AoE templates) — core BE never owns map/spatial state and
 * does not resolve spells through this DTO.
 *
 * <p>Values are deliberately forgiving: where the underlying data is not reliably structured the
 * field is left null and the enum-like string carries {@code UNKNOWN}. Nothing here is parsed out
 * of free localized text.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TacticalActionResponse {

    /** Stable identifier of the source option (spell UUID, feature UUID, or attack name as a fallback). */
    private String id;

    private String name;

    /** WEAPON | SPELL | CLASS_ABILITY | MONSTER_FEATURE | MANUAL. */
    private String source;

    /** ACTION | BONUS_ACTION | REACTION | FREE | UNKNOWN. */
    private String actionCost;

    private Targeting targeting;

    private List<Damage> damage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Targeting {
        /** SELF | SINGLE_TARGET | AREA | LINE | CONE | UNKNOWN. */
        private String mode;
        /** Distance to target/origin in feet when reliably structured; null otherwise. */
        private Integer rangeFt;
        /** RADIUS | CONE | LINE | RECTANGLE | UNKNOWN. */
        private String areaShape;
        private Integer radiusFt;
        private Integer widthFt;
        private Integer lengthFt;
        private boolean requiresAttackRoll;
        private boolean requiresSavingThrow;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Damage {
        private String dice;
        private String damageType;
    }
}
