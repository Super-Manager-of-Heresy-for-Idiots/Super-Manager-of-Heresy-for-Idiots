package com.dnd.app.service;

import com.dnd.app.domain.BattleCombatant;
import com.dnd.app.domain.PlayerCharacter;
import com.dnd.app.domain.enums.BattleStatus;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.dto.combat.HpChangeResult;
import com.dnd.app.exception.ResourceNotFoundException;
import com.dnd.app.repository.BattleCombatantRepository;
import com.dnd.app.repository.PlayerCharacterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * The single primitive for changing a character's hit points. Every write path — attacks, item use,
 * GM adjustments, feature resolution and rest — must go through here so temp-HP absorption, the
 * pessimistic write lock, live combat-tracker mirroring and the {@code HP_CHANGED} broadcast happen
 * exactly once and identically. Extracted from {@code BattleService.applyDamageOrHeal} so the
 * feature-rules runtime shares the same accounting instead of writing {@code current_hp} directly.
 *
 * <p>Lock order: callers that already hold combat locks acquire them as battle → combatant → this
 * character lock, so joining an existing transaction here cannot invert the order.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterHpService {

    private final PlayerCharacterRepository characterRepository;
    private final BattleCombatantRepository combatantRepository;
    private final WebSocketEventService webSocketEventService;
    private final GameplayEventService gameplayEventService;

    /**
     * Applies a signed HP {@code delta} to a character (negative damages, positive heals). Temp HP
     * absorbs damage first; healing is capped at max HP. The character is loaded under a pessimistic
     * write lock so simultaneous changes accumulate. Every active combat tracker for the character is
     * mirrored, an {@code HP_CHANGED} event is broadcast (skipped when {@code campaignId} is null),
     * and dropping to 0 publishes an {@code hp_reached_zero} gameplay event (a hook for death/
     * concentration; a no-op unless the triggers subsystem is enabled).
     */
    @Transactional
    public HpChangeResult applyDelta(UUID characterId, int delta, UUID campaignId, UUID actorUserId) {
        PlayerCharacter character = characterRepository.findByIdForUpdate(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));

        Integer maxHpObj = character.getMaxHp();
        int cap = maxHpObj != null ? maxHpObj : 0;
        int before = character.getCurrentHp() != null ? character.getCurrentHp() : 0;

        character.applyHpDelta(delta, cap);
        characterRepository.save(character);

        int currentHp = character.getCurrentHp() != null ? character.getCurrentHp() : 0;
        int tempHp = character.getTempHp() != null ? character.getTempHp() : 0;

        syncCombatants(characterId, currentHp, maxHpObj);
        broadcast(character.getId(), campaignId, actorUserId, currentHp, tempHp, cap);

        boolean reachedZero = before > 0 && currentHp <= 0;
        if (reachedZero) {
            // Durable hook so death saves / concentration can attach later; no-op unless triggers on.
            gameplayEventService.publish(character, "hp_reached_zero", null,
                    "{\"characterId\":\"" + characterId + "\"}");
        }
        return new HpChangeResult(characterId, currentHp, tempHp, cap, reachedZero);
    }

    /**
     * Convenience for damage that carries a type. Resistance/vulnerability is not yet applied here —
     * that is wired in when the modifier aggregator lands ({@code ModifierAggregator}); for now this
     * clamps to non-negative and applies it as a delta so callers can already thread a damage type.
     */
    @Transactional
    public HpChangeResult applyDamage(UUID characterId, int amount, UUID damageTypeId,
                                      UUID campaignId, UUID actorUserId) {
        return applyDelta(characterId, -Math.max(0, amount), campaignId, actorUserId);
    }

    /**
     * Grants temporary HP (does not stack — the larger pool wins). Temp HP lives only on the sheet,
     * so combat trackers (which store current/max only) are not mirrored, but an {@code HP_CHANGED}
     * event is still broadcast so views refresh.
     */
    @Transactional
    public HpChangeResult applyTempHp(UUID characterId, int amount, UUID campaignId, UUID actorUserId) {
        PlayerCharacter character = characterRepository.findByIdForUpdate(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));

        character.grantTempHp(Math.max(0, amount));
        characterRepository.save(character);

        int currentHp = character.getCurrentHp() != null ? character.getCurrentHp() : 0;
        int tempHp = character.getTempHp() != null ? character.getTempHp() : 0;
        int cap = character.getMaxHp() != null ? character.getMaxHp() : 0;

        broadcast(character.getId(), campaignId, actorUserId, currentHp, tempHp, cap);
        return new HpChangeResult(characterId, currentHp, tempHp, cap, false);
    }

    /**
     * Restores a character to full HP and clears temporary HP — the HP half of a long rest. Uses the
     * same lock, tracker mirroring and broadcast as {@link #applyDelta}. When max HP is unknown the
     * current value is left as the ceiling so healing is never negative.
     */
    @Transactional
    public HpChangeResult restoreToFull(UUID characterId, UUID campaignId, UUID actorUserId) {
        PlayerCharacter character = characterRepository.findByIdForUpdate(characterId)
                .orElseThrow(() -> new ResourceNotFoundException("Character not found"));

        Integer maxObj = character.getMaxHp();
        int full = maxObj != null ? maxObj : (character.getCurrentHp() != null ? character.getCurrentHp() : 0);
        character.setCurrentHp(full);
        character.setTempHp(0);
        characterRepository.save(character);

        syncCombatants(characterId, full, maxObj);
        broadcast(character.getId(), campaignId, actorUserId, full, 0, full);
        return new HpChangeResult(characterId, full, 0, maxObj != null ? maxObj : full, false);
    }

    private void syncCombatants(UUID characterId, int currentHp, Integer maxHp) {
        for (BattleCombatant combatant :
                combatantRepository.findByCharacter_IdAndBattle_Status(characterId, BattleStatus.ACTIVE)) {
            combatant.setCurrentHp(currentHp);
            if (maxHp != null) {
                combatant.setMaxHp(maxHp);
            }
            combatantRepository.save(combatant);
        }
    }

    private void broadcast(UUID characterId, UUID campaignId, UUID actorUserId,
                           int currentHp, int tempHp, int maxHp) {
        webSocketEventService.sendCampaignEvent(WebSocketEventType.HP_CHANGED, campaignId, characterId,
                Map.of("currentHp", currentHp, "tempHp", tempHp, "maxHp", maxHp), actorUserId);
    }
}
