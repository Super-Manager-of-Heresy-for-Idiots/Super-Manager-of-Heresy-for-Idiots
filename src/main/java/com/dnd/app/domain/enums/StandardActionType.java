package com.dnd.app.domain.enums;

/**
 * The economy-spending standard actions of D&D 5e that resolve to a turn-scoped combatant state.
 * DASH doubles movement; DODGE imposes disadvantage on attackers (advantage on Dex saves);
 * DISENGAGE suppresses opportunity attacks; HELP grants an ally advantage on its next attack;
 * HIDE is a Stealth check that, on success, hides the combatant. (Grapple/Shove are opposed
 * contests handled separately.)
 */
public enum StandardActionType {
    DASH,
    DODGE,
    DISENGAGE,
    HELP,
    HIDE
}
