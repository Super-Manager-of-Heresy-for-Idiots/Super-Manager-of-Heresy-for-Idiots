package com.dnd.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Класс BattleCombatantResponse описывает DTO ответа, который возвращает результат бизнес-сценария клиенту.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BattleCombatantResponse {

    private UUID id;
    private String type;
    private String displayName;
    private UUID monsterId;
    private UUID characterId;
    private UUID ownerUserId;
    private Integer instanceIndex;
    private Integer initiative;
    private Integer initiativeRoll;
    private Integer turnOrder;
    private Integer currentHp;
    private Integer maxHp;
    private boolean currentTurn;

    // Action economy for the current turn. Actions and bonus actions are pools (max + how many
    // spent this turn); legendary actions default to 0. Reactions stay a single per-round flag.
    private int actionMax;
    private int actionSpent;
    private int bonusActionMax;
    private int bonusActionSpent;
    private int legendaryActionMax;
    private int legendaryActionSpent;
    private boolean reactionUsed;

    /** Live conditions on this combatant (Blinded, Prone, …); empty when none. */
    @Builder.Default
    private java.util.List<CombatantConditionResponse> conditions = new java.util.ArrayList<>();

    // Death saves for a dying character (0 HP). Both 0 for monsters and healthy characters.
    @Builder.Default
    private int deathSaveSuccesses = 0;
    @Builder.Default
    private int deathSaveFailures = 0;
    /** True when the character is dead (three death-save failures). */
    private boolean dead;
    /** True when the character is currently concentrating on a spell (Phase 2.2). */
    private boolean concentrating;
    /** DC of a pending concentration save the player must roll (Phase 2.2); null when none pending. */
    private Integer pendingConcentrationDc;

    // ---- Standard-action turn state (Phase 2.7) ------------------------------------------------
    /** Dash taken: movement budget doubled this turn. */
    private boolean dashing;
    /** Dodging: attackers have disadvantage, this combatant has advantage on Dex saves. */
    private boolean dodging;
    /** Disengaged: movement provokes no opportunity attacks this turn. */
    private boolean disengaged;
    /** Hidden from enemies; its next attack has advantage, then it is revealed. */
    private boolean hidden;
    /** An ally Helped this combatant: its next attack has advantage (consumed on use). */
    private boolean helpAdvantage;

    // ---- Monster runtime (Phase 2.9) -----------------------------------------------------------
    /** Legendary Resistance uses per day (0 when the monster has none). */
    private int legendaryResistanceMax;
    /** Legendary Resistance uses already spent. */
    private int legendaryResistanceUsed;
    /** Attacks a Multiattack monster may still make this turn; null for single-attack combatants. */
    private Integer attacksRemaining;

    // ---- Hidden identity (Phase 2.10) ----------------------------------------------------------
    /** True when this monster's identity is hidden from players (the GM still sees {@link #displayName}). */
    private boolean identityHidden;
    /** Generic public label shown to players when the identity is hidden (e.g. "Неизвестное существо #2"). */
    private String publicName;

    /** Manual GM speed override in feet (Phase 2.11); null when the sheet/statblock speed applies. */
    private Integer speedOverrideFt;

    // ---- Flight (Phase 2.13) -------------------------------------------------------------------
    /** Persistent flying state: the creature is aloft (stays flying between turns). */
    private boolean flying;
    /** True when the creature can hover (from the monster statblock) — it does not fall at 0 fly speed. */
    private boolean hover;
    /** Внезапность (фаза 3.7): застигнут врасплох — в первом раунде не может действовать/реагировать. */
    private boolean surprised;
    /** Подготовленное действие (Ready, фаза 3.7): текст отложенного действия; null — нет. */
    private String readiedAction;
}
