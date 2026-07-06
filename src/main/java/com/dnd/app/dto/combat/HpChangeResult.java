package com.dnd.app.dto.combat;

import java.util.UUID;

/**
 * Authoritative outcome of a single HP mutation applied through {@code CharacterHpService}. Callers
 * (battle actions, feature resolution, rest) use it to build their own responses without re-reading
 * the character row.
 *
 * @param characterId the character whose HP changed
 * @param currentHp   current HP after the change (floored at 0)
 * @param tempHp      temporary HP remaining after the change
 * @param maxHp       max HP used as the healing cap (0 when unknown)
 * @param reachedZero {@code true} when this change dropped the character from above 0 to 0
 */
public record HpChangeResult(UUID characterId, int currentHp, int tempHp, int maxHp, boolean reachedZero) {
}
