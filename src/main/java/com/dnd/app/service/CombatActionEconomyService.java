package com.dnd.app.service;

import com.dnd.app.domain.BattleCombatant;
import com.dnd.app.domain.enums.WebSocketEventType;
import com.dnd.app.exception.BadRequestException;
import com.dnd.app.repository.BattleCombatantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The action economy for callers OUTSIDE the core attack flow — chiefly the feature-rules runtime, so
 * that using a feature in combat actually costs its declared slot instead of being free. Keyed by
 * {@code (combatId, characterId)} because a feature is always used by a character; {@code combatId}
 * is the battle id. Feature action codes that carry no economy cost ({@code free_action},
 * {@code no_action}, {@code special}) consume nothing.
 *
 * <p>Not flag-gated itself — gating lives at the callers ({@code FeatureUseService} is gated by
 * {@code app.feature-rules.actions}; reaction prompts by triggers). {@code BattleService} keeps its
 * own inline economy for the hot attack/item/spend paths; folding those onto this service is a
 * deliberate follow-up so this change does not disturb that well-tested flow.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CombatActionEconomyService {

    /** A per-turn economy slot a feature cost can consume. */
    public enum Slot { ACTION, BONUS_ACTION, REACTION }

    private final BattleCombatantRepository combatantRepository;
    private final WebSocketEventService webSocketEventService;

    /**
     * Spends the slot mapped from {@code actionTypeCode} for the character's combatant in the given
     * combat. Cost-free codes are a no-op. Throws {@link BadRequestException} when the character is not
     * a combatant in that battle or the slot is already used this turn — invoke this BEFORE any
     * irreversible spend so the surrounding transaction rolls back cleanly on failure.
     */
    @Transactional
    public void spend(UUID combatId, UUID characterId, String actionTypeCode) {
        Slot slot = slotForCode(actionTypeCode);
        if (slot == null) {
            return; // free_action / no_action / special: nothing to spend
        }
        BattleCombatant combatant = combatantRepository
                .findByBattleIdAndCharacterIdForUpdate(combatId, characterId)
                .orElseThrow(() -> new BadRequestException("Персонаж не участвует в этом бою"));
        applySlot(combatant, slot);
        combatantRepository.save(combatant);
        broadcast(combatant, slot);
        log.debug("Feature action economy spent: combat={}, character={}, slot={}", combatId, characterId, slot);
    }

    /**
     * Whether the mapped slot is still free this turn. Cost-free codes are always spendable; a
     * character not present in the combat is not.
     */
    @Transactional(readOnly = true)
    public boolean canSpend(UUID combatId, UUID characterId, String actionTypeCode) {
        Slot slot = slotForCode(actionTypeCode);
        if (slot == null) {
            return true;
        }
        return combatantRepository.findByBattleIdAndCharacterId(combatId, characterId)
                .map(c -> hasSlot(c, slot))
                .orElse(false);
    }

    /** Maps a feature {@code action_type} code to an economy slot, or {@code null} when it costs none. */
    public static Slot slotForCode(String code) {
        if (code == null) {
            return null;
        }
        return switch (code) {
            case "action" -> Slot.ACTION;
            case "bonus_action" -> Slot.BONUS_ACTION;
            case "reaction" -> Slot.REACTION;
            default -> null; // free_action, no_action, special
        };
    }

    private boolean hasSlot(BattleCombatant c, Slot slot) {
        return switch (slot) {
            case ACTION -> c.getActionSpent() < c.getActionMax();
            case BONUS_ACTION -> c.getBonusActionSpent() < c.getBonusActionMax();
            case REACTION -> !Boolean.TRUE.equals(c.getReactionUsed());
        };
    }

    private void applySlot(BattleCombatant c, Slot slot) {
        switch (slot) {
            case ACTION -> {
                if (c.getActionSpent() >= c.getActionMax()) {
                    throw new BadRequestException("Действие в этом раунде уже потрачено");
                }
                c.setActionSpent(c.getActionSpent() + 1);
            }
            case BONUS_ACTION -> {
                if (c.getBonusActionSpent() >= c.getBonusActionMax()) {
                    throw new BadRequestException("Бонусное действие в этом раунде уже потрачено");
                }
                c.setBonusActionSpent(c.getBonusActionSpent() + 1);
            }
            case REACTION -> {
                if (Boolean.TRUE.equals(c.getReactionUsed())) {
                    throw new BadRequestException("Реакция в этом раунде уже использована");
                }
                c.setReactionUsed(true);
            }
        }
    }

    private void broadcast(BattleCombatant c, Slot slot) {
        UUID campaignId = c.getBattle() != null && c.getBattle().getCampaign() != null
                ? c.getBattle().getCampaign().getId() : null;
        if (campaignId == null) {
            return;
        }
        webSocketEventService.sendCampaignEvent(WebSocketEventType.BATTLE_ACTION, campaignId,
                Map.of("battleId", c.getBattle().getId(), "combatantId", c.getId(),
                        "slot", slot.name().toLowerCase(), "economy", "spent"), null);
    }
}
