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
 * Класс CombatActionEconomyService описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
 * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CombatActionEconomyService {

    /**
     * Перечисление Slot описывает сервис бизнес-логики, который координирует правила домена и работу с данными.
     * Используется для сохранения явной роли элемента в бизнес-потоке приложения.
     */
    public enum Slot { ACTION, BONUS_ACTION, REACTION }

    private final BattleCombatantRepository combatantRepository;
    private final WebSocketEventService webSocketEventService;

    /**
     * Выполняет операции "spend" в рамках бизнес-логики домена.
     * @param combatId идентификатор combat, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param actionTypeCode входящее значение action type code, используемое бизнес-сценарием
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
     * Проверяет условие операции "can spend" в рамках бизнес-логики домена.
     * @param combatId идентификатор combat, используемый для выбора нужного бизнес-объекта
     * @param characterId идентификатор character, используемый для выбора нужного бизнес-объекта
     * @param actionTypeCode входящее значение action type code, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
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

    /**
     * Выполняет операции "slot for code" в рамках бизнес-логики домена.
     * @param code входящее значение code, используемое бизнес-сценарием
     * @return результат выполнения бизнес-операции
     */
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
